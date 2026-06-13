package com.example.authapp.domain.mfa.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.authapp.domain.admin.AdminSettingsStore;
import com.example.authapp.domain.jwt.service.RefreshTokenService;
import com.example.authapp.domain.mfa.dto.MfaChallengeResult;
import com.example.authapp.domain.mfa.dto.MfaLoginDecision;
import com.example.authapp.domain.mfa.dto.MfaMethodResponse;
import com.example.authapp.domain.mfa.dto.MfaVerifyRequest;
import com.example.authapp.domain.mfa.dto.PreAuthTotpConfirmRequest;
import com.example.authapp.domain.mfa.dto.PreAuthTotpSetupResponse;
import com.example.authapp.domain.mfa.dto.TotpConfirmRequest;
import com.example.authapp.domain.mfa.dto.TotpSetupResponse;
import com.example.authapp.domain.mfa.entity.MfaChallengeEntity;
import com.example.authapp.domain.mfa.entity.MfaMethodEntity;
import com.example.authapp.domain.mfa.entity.MfaMethodType;
import com.example.authapp.domain.mfa.entity.MfaPolicy;
import com.example.authapp.domain.mfa.exception.MfaException;
import com.example.authapp.domain.mfa.repository.MfaChallengeRepository;
import com.example.authapp.domain.mfa.repository.MfaExceptionRepository;
import com.example.authapp.domain.mfa.repository.MfaMethodRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class MfaService {

    private static final int MAX_CHALLENGE_FAILURES = 5;

    private final MfaMethodRepository mfaMethodRepository;
    private final MfaChallengeRepository mfaChallengeRepository;
    private final MfaExceptionRepository mfaExceptionRepository;
    private final AdminSettingsStore adminSettingsStore;
    private final TotpService totpService;
    private final RefreshTokenService refreshTokenService;

    
    // MFA 로그인 필요 여부를 판단한다.
    @Transactional(readOnly = true)
    public MfaLoginDecision evaluateLogin(String username, Collection<String> roles) {
        List<MfaMethodEntity> methods = mfaMethodRepository.findByUsernameAndEnabledTrue(username);
        // 이미 등록된 MFA 수단이 있으면 로그인 시 MFA 검증 필요
        if (!methods.isEmpty()) {
            return new MfaLoginDecision(
                    true,
                    false,
                    methods.stream().map(MfaMethodEntity::getType).distinct().toList()
            );
        }
        // 등록된 MFA가 없고 예외 대상이면 MFA 불필요
        if (mfaExceptionRepository.findActiveByUsername(username).isPresent()) {
            return MfaLoginDecision.notRequired();
        }

        MfaPolicy policy = adminSettingsStore.mfaPolicy();
        
        // 정책상 MFA 필수 대상이면 MFA 등록 플로우 필요
        if (policy.normalized() == MfaPolicy.OPTIONAL || !policyRequiresUser(policy, roles)) {
            return MfaLoginDecision.notRequired();
        }
        return MfaLoginDecision.requireRegistration();
    }
    
    // 특정 사용자의 MFA 등록 정보와 열려 있는 challenge를 초기화
    // 관리자에 의한 MFA 초기화, 사용자 보안 복구 등의 상황에서 사용할 수 있다.
    public void resetUserMfa(String username) {
        mfaMethodRepository.deleteByUsername(username);
        mfaChallengeRepository.expireOpenChallenges(username);
    }

    // 로그인 이후 사용자가 직접 TOTP를 등록할 때 사용
    public TotpSetupResponse setupTotp(String username) {
    	// 아직 확정되지 않은 기존 TOTP 등록 시도는 제거
        mfaMethodRepository.deleteByUsernameAndTypeAndEnabledFalse(username, MfaMethodType.TOTP);
        // 새 secret과 QR 코드를 발급한다.
        String secret = totpService.generateSecret();
        MfaMethodEntity method = mfaMethodRepository.save(MfaMethodEntity.builder()
                .username(username)
                .type(MfaMethodType.TOTP)
                .enabled(false)
                .secret(secret)
                .build());
        String uri = totpService.otpAuthUri(username, secret);
        return new TotpSetupResponse(method.getId(), secret, uri, totpService.qrCodeDataUri(uri));
    }

    // 로그인 이후 TOTP 등록을 확정
    // 사용자가 QR을 인증 앱에 등록한 뒤 입력한 6자리 코드를 검증 -> 성공하면 해당 MFA method를 활성화한다.
    public MfaMethodResponse confirmTotp(String username, TotpConfirmRequest request) {
        MfaMethodEntity method = mfaMethodRepository.findById(request.methodId())
                .filter(item -> item.getUsername().equals(username))
                .filter(item -> item.getType() == MfaMethodType.TOTP)
                .orElseThrow(() -> MfaException.badRequest("MFA_METHOD_NOT_FOUND", "MFA method not found."));
        if (method.isEnabled()) {
            throw MfaException.badRequest("MFA_METHOD_ALREADY_REGISTERED", "MFA method is already registered.");
        }
        if (!totpService.verify(method.getSecret(), request.code())) {
            throw MfaException.badRequest("MFA_CODE_INVALID", "Invalid MFA code.");
        }
        mfaMethodRepository.deleteActiveByUsernameAndType(username, MfaMethodType.TOTP);
        method.confirmRegistration();
        return MfaMethodResponse.from(method);
    }

    // 사용자의 MFA method 목록을 조회
    @Transactional(readOnly = true)
    public List<MfaMethodResponse> methods(String username) {
        return mfaMethodRepository.findByUsername(username)
                .stream()
                .map(MfaMethodResponse::from)
                .toList();
    }

    // 현재 TOTP 코드를 재검증한 뒤 MFA method를 삭제
    public void deleteMethod(String username, Long id, String code) {
        MfaMethodEntity method = mfaMethodRepository.findById(id)
                .filter(item -> item.getUsername().equals(username))
                .orElseThrow(() -> MfaException.badRequest("MFA_METHOD_NOT_FOUND", "MFA method not found."));
        
        // 사용자가 자신의 MFA를 삭제할 때 탈취된 세션만으로 MFA가 제거되는 것을 막기 위한 방어 로직
        if (method.getType() == MfaMethodType.TOTP && !totpService.verify(method.getSecret(), code)) {
            throw MfaException.badRequest("MFA_CODE_INVALID", "Invalid MFA code.");
        }
        mfaMethodRepository.delete(method);
    }

    
    // MFA 검증 또는 MFA 등록에 사용할 challenge를 생성
    public MfaChallengeResult createChallenge(String username, MfaLoginDecision decision, String ipAddress, String userAgent) {
        // challenge는 ID/PW 1차 인증 이후 JWT 발급 전에 만들어진다.
    	String challengeId = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(5);
        mfaChallengeRepository.save(MfaChallengeEntity.builder()
                .challengeId(challengeId)
                .username(username)
                .expiresAt(expiresAt)
                .verified(false)
                .used(false)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .requestFingerprint(fingerprint(ipAddress, userAgent))
                .build());
        // 이후 TOTP 검증 또는 pre-auth TOTP 등록 과정에서 challengeId로 사용자를 식별한다.
        return new MfaChallengeResult(challengeId, expiresAt, decision.registrationRequired(), decision.availableMethods());
    }

	 // 인증 전 상태에서 TOTP 등록을 시작
    @Transactional(noRollbackFor = MfaException.class)
    public PreAuthTotpSetupResponse setupPreAuthTotpRegistration(String challengeId, String ipAddress, String userAgent) {
    	// 아직 JWT를 발급하지 않은 상태이므로 challengeId로 사용자를 식별한다.
    	MfaChallengeEntity challenge = openChallenge(challengeId);
        assertUsableChallenge(challenge, ipAddress, userAgent);
        
        //기존 미완료 TOTP 등록 시도는 제거하고 새 secret, QR 코드, methodId를 반환한다.
        mfaMethodRepository.deleteByUsernameAndTypeAndEnabledFalse(challenge.getUsername(), MfaMethodType.TOTP);        
        String secret = totpService.generateSecret();
        MfaMethodEntity method = mfaMethodRepository.save(MfaMethodEntity.builder()
                .username(challenge.getUsername())
                .type(MfaMethodType.TOTP)
                .enabled(false)
                .secret(secret)
                .build());
        String uri = totpService.otpAuthUri(challenge.getUsername(), secret);
        return new PreAuthTotpSetupResponse(challengeId, method.getId(), secret, uri, totpService.qrCodeDataUri(uri), challenge.getExpiresAt());
    }

    //로그인 완료 전 TOTP 등록을 확정
    @Transactional(noRollbackFor = MfaException.class)
    public String confirmPreAuthTotpRegistration(PreAuthTotpConfirmRequest request, String ipAddress, String userAgent) {
        // 사용자가 인증 앱에 QR을 등록한 뒤 입력한 코드를 검증한다.
    	MfaChallengeEntity challenge = openChallenge(request.challengeId());
        assertUsableChallenge(challenge, ipAddress, userAgent);

        // 성공하면 TOTP method를 활성화하고 challenge를 사용 완료 처리한다.
        MfaMethodEntity method = mfaMethodRepository.findById(request.methodId())
                .filter(item -> item.getUsername().equals(challenge.getUsername()))
                .filter(item -> item.getType() == MfaMethodType.TOTP)
                .filter(item -> !item.isEnabled())
                .orElseThrow(() -> MfaException.badRequest("MFA_METHOD_NOT_FOUND", "MFA method not found."));
        verifyCodeOrRecordFailure(challenge, method, request.code());

        // 반환된 username은 AuthFacade에서 최종 JWT 발급에 사용된다.
        mfaMethodRepository.deleteActiveByUsernameAndType(challenge.getUsername(), MfaMethodType.TOTP);
        method.confirmRegistration();
        challenge.markVerifiedAndUsed();
        return challenge.getUsername();
    }
    
    // 로그인 시 발급된 MFA challenge를 검증
    @Transactional(noRollbackFor = MfaException.class)
    public String verifyChallenge(MfaVerifyRequest request, String ipAddress, String userAgent) {
    	// challengeId로 잠금 조회한 뒤 만료 여부와 요청 컨텍스트를 확인한다.
        MfaChallengeEntity challenge = openChallenge(request.challengeId());
        assertUsableChallenge(challenge, ipAddress, userAgent);
        
        // TOTP 코드 검증 성공 시 challenge를 사용 완료 처리하고 username을 반환한다.
        if (request.methodType() != MfaMethodType.TOTP) {
            throw MfaException.badRequest("MFA_METHOD_UNSUPPORTED", "Unsupported MFA method.");
        }
        MfaMethodEntity method = mfaMethodRepository.findByUsernameAndTypeAndEnabledTrue(challenge.getUsername(), MfaMethodType.TOTP)
                .orElseThrow(() -> MfaException.badRequest("MFA_TOTP_NOT_REGISTERED", "TOTP is not registered."));
        verifyCodeOrRecordFailure(challenge, method, request.code());
        method.markUsed();
        challenge.markVerifiedAndUsed();
        return challenge.getUsername();
    }

    //아직 사용되지 않은 challenge를 잠금 조회
    //repository의 PESSIMISTIC_WRITE 조회를 사용해 같은 challengeId가 동시에 검증되는 race condition을 방지한다. 
    private MfaChallengeEntity openChallenge(String challengeId) {
        return mfaChallengeRepository.findByChallengeIdAndUsedFalseForUpdate(challengeId)
                .orElseThrow(() -> MfaException.badRequest("MFA_CHALLENGE_NOT_FOUND", "MFA challenge not found."));
    }

    //challenge가 검증 가능한 상태인지 확인
    private void assertUsableChallenge(MfaChallengeEntity challenge, String ipAddress, String userAgent) {
        // 만료된 challenge는 즉시 사용 처리
    	if (challenge.isExpired()) {
            challenge.markUsed();
            throw MfaException.badRequest("MFA_CHALLENGE_EXPIRED", "MFA challenge expired.");
        }
    	// IP/User-Agent가 제공된 경우 최초 challenge 생성 시점의 fingerprint와 비교해 다른 요청 컨텍스트에서 이어지는 MFA 검증을 차단
        if (ipAddress != null && userAgent != null && !fingerprint(ipAddress, userAgent).equals(challenge.getRequestFingerprint())) {
            challenge.markUsed();
            throw MfaException.badRequest("MFA_CONTEXT_CHANGED", "MFA verification context changed. Please sign in again.");
        }
    }

    // TOTP 코드를 검증하고 실패 횟수를 기록
    private void verifyCodeOrRecordFailure(MfaChallengeEntity challenge, MfaMethodEntity method, String code) {
        // 성공하면 바로 반환
    	if (totpService.verify(method.getSecret(), code)) {
            return;
        }
    	// 실패하면 challenge의 실패 횟수를 증가시키고,최대 실패 횟수에 도달하면 challenge를 폐기하고 모든 refresh token을 revoke한다.
        int failedAttempts = challenge.recordFailedAttempt();
        if (failedAttempts >= MAX_CHALLENGE_FAILURES) {
            challenge.markUsed();
            refreshTokenService.revokeAllByUsername(challenge.getUsername(), "MFA_FAILED_LIMIT");
            throw MfaException.badRequest("MFA_FAILED_LIMIT", "MFA verification failed 5 times. All sessions were revoked. Please sign in again.");
        }
        int remainingAttempts = MAX_CHALLENGE_FAILURES - failedAttempts;
        throw MfaException.badRequest(
                "MFA_CODE_INVALID",
                "Invalid MFA code. Failed " + failedAttempts + "/" + MAX_CHALLENGE_FAILURES
                        + ". Remaining attempts: " + remainingAttempts + "."
        );
    }

    //현재 MFA 정책이 해당 사용자 역할에 적용되는지 판단
    private boolean policyRequiresUser(MfaPolicy policy, Collection<String> roles) {
        MfaPolicy normalized = policy.normalized();
        // REQUIRED_FOR_ALL이면 모든 사용자에게 적용
        if (normalized == MfaPolicy.REQUIRED_FOR_ALL) {
            return true;
        }
        // REQUIRED_FOR_ADMIN이면 ROLE_ADMIN 또는 ADMIN 역할을 가진 사용자에게만 적용
        return normalized == MfaPolicy.REQUIRED_FOR_ADMIN
                && roles.stream().anyMatch(role -> "ROLE_ADMIN".equals(role) || "ADMIN".equals(role));
    }

    // 요청 컨텍스트 검증용 fingerprint를 생성
    private String fingerprint(String ipAddress, String userAgent) {
    	// IP와 User-Agent를 합친 뒤 SHA-256으로 해시하고 Base64 URL-safe 문자열로 변환 -> 원본 IP/User-Agent를 직접 비교하지 않고 fingerprint를 비교하기 위한 값이다.
        try {
            String source = (ipAddress == null ? "" : ipAddress) + "|" + (userAgent == null ? "" : userAgent);
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(source.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create MFA request fingerprint.", e);
        }
    }
}
