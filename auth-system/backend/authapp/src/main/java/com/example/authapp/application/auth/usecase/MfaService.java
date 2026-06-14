package com.example.authapp.application.auth.usecase;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.authapp.domain.audit.service.AuthEventLogService;
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
import com.example.authapp.domain.mfa.service.MfaSettingsService;
import com.example.authapp.domain.mfa.service.TotpService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class MfaService {

    private final MfaMethodRepository mfaMethodRepository;
    private final MfaChallengeRepository mfaChallengeRepository;
    private final MfaExceptionRepository mfaExceptionRepository;
    private final MfaSettingsService mfaSettingsService;
    private final TotpService totpService;
    private final RefreshTokenService refreshTokenService;
    private final AuthEventLogService authEventLogService;

    // 로그인 시 MFA 필요 여부를 판단한다.
    @Transactional(readOnly = true)
    public MfaLoginDecision evaluateLogin(String username, Collection<String> roles) {
        // 등록된 MFA 수단이 있으면 로그인 MFA 검증을 요구한다.
        List<MfaMethodEntity> methods = mfaMethodRepository.findByUsernameAndEnabledTrue(username);
        if (!methods.isEmpty()) {
            return new MfaLoginDecision(
                    true,
                    false,
                    methods.stream().map(MfaMethodEntity::getType).distinct().toList()
            );
        }

        // 활성 MFA 예외가 있으면 MFA를 요구하지 않는다.
        if (mfaExceptionRepository.findActiveByUsername(username).isPresent()) {
            return MfaLoginDecision.notRequired();
        }

        // 정책상 MFA 대상이면 MFA 등록 흐름을 요구한다.
        MfaPolicy policy = mfaSettingsService.policy();
        if (policy.normalized() == MfaPolicy.OPTIONAL || !policyRequiresUser(policy, roles)) {
            return MfaLoginDecision.notRequired();
        }
        return MfaLoginDecision.requireRegistration();
    }

    // 사용자의 MFA 등록 상태를 초기화한다.
    public void resetUserMfa(String username) {
        // 사용자의 MFA 등록 정보와 열린 challenge를 초기화한다.
        mfaMethodRepository.deleteByUsername(username);
        mfaChallengeRepository.expireOpenChallenges(username);
    }

    // 로그인 후 TOTP 등록 정보를 발급한다.
    public TotpSetupResponse setupTotp(String username) {
        // 미완료 TOTP 등록 시도를 정리한다.
        mfaMethodRepository.deleteByUsernameAndTypeAndEnabledFalse(username, MfaMethodType.TOTP);

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

    // 로그인 후 TOTP 등록을 확정한다.
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
        authEventLogService.mfaRegistered(username, MfaMethodType.TOTP.name());
        return MfaMethodResponse.from(method);
    }

    // 사용자의 MFA 수단 목록을 조회한다.
    @Transactional(readOnly = true)
    public List<MfaMethodResponse> methods(String username) {
        return mfaMethodRepository.findByUsername(username)
                .stream()
                .map(MfaMethodResponse::from)
                .toList();
    }

    // 사용자의 MFA 수단을 삭제한다.
    public void deleteMethod(String username, Long id, String code) {
        MfaMethodEntity method = mfaMethodRepository.findById(id)
                .filter(item -> item.getUsername().equals(username))
                .orElseThrow(() -> MfaException.badRequest("MFA_METHOD_NOT_FOUND", "MFA method not found."));

        if (method.getType() == MfaMethodType.TOTP && !totpService.verify(method.getSecret(), code)) {
            throw MfaException.badRequest("MFA_CODE_INVALID", "Invalid MFA code.");
        }
        mfaMethodRepository.delete(method);
    }

    // MFA 검증 challenge를 생성한다.
    public MfaChallengeResult createChallenge(String username, MfaLoginDecision decision, String ipAddress, String userAgent) {
        String challengeId = UUID.randomUUID().toString();
        var settings = mfaSettingsService.current();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(settings.getChallengeExpirationMinutes());
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
        return new MfaChallengeResult(challengeId, expiresAt, decision.registrationRequired(), decision.availableMethods());
    }

    @Transactional(noRollbackFor = MfaException.class)
    // 인증 전 TOTP 등록 challenge를 준비한다.
    public PreAuthTotpSetupResponse setupPreAuthTotpRegistration(String challengeId, String ipAddress, String userAgent) {
        MfaChallengeEntity challenge = openChallenge(challengeId);
        assertUsableChallenge(challenge, ipAddress, userAgent);

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

    @Transactional(noRollbackFor = MfaException.class)
    // 인증 전 TOTP 등록 challenge를 확정한다.
    public String confirmPreAuthTotpRegistration(PreAuthTotpConfirmRequest request, String ipAddress, String userAgent) {
        MfaChallengeEntity challenge = openChallenge(request.challengeId());
        assertUsableChallenge(challenge, ipAddress, userAgent);

        MfaMethodEntity method = mfaMethodRepository.findById(request.methodId())
                .filter(item -> item.getUsername().equals(challenge.getUsername()))
                .filter(item -> item.getType() == MfaMethodType.TOTP)
                .filter(item -> !item.isEnabled())
                .orElseThrow(() -> MfaException.badRequest("MFA_METHOD_NOT_FOUND", "MFA method not found."));
        verifyCodeOrRecordFailure(challenge, method, request.code());

        mfaMethodRepository.deleteActiveByUsernameAndType(challenge.getUsername(), MfaMethodType.TOTP);
        method.confirmRegistration();
        challenge.markVerifiedAndUsed();
        authEventLogService.mfaRegistered(challenge.getUsername(), MfaMethodType.TOTP.name());
        return challenge.getUsername();
    }

    @Transactional(noRollbackFor = MfaException.class)
    // MFA challenge를 검증하고 사용자명을 반환한다.
    public String verifyChallenge(MfaVerifyRequest request, String ipAddress, String userAgent) {
        MfaChallengeEntity challenge = openChallenge(request.challengeId());
        assertUsableChallenge(challenge, ipAddress, userAgent);

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

    private MfaChallengeEntity openChallenge(String challengeId) {
        return mfaChallengeRepository.findByChallengeIdAndUsedFalseForUpdate(challengeId)
                .orElseThrow(() -> MfaException.badRequest("MFA_CHALLENGE_NOT_FOUND", "MFA challenge not found."));
    }

    private void assertUsableChallenge(MfaChallengeEntity challenge, String ipAddress, String userAgent) {
        if (challenge.isExpired()) {
            challenge.markUsed();
            throw MfaException.badRequest("MFA_CHALLENGE_EXPIRED", "MFA challenge expired.");
        }

        if (ipAddress != null && userAgent != null && !fingerprint(ipAddress, userAgent).equals(challenge.getRequestFingerprint())) {
            challenge.markUsed();
            throw MfaException.badRequest("MFA_CONTEXT_CHANGED", "MFA verification context changed. Please sign in again.");
        }
    }

    private void verifyCodeOrRecordFailure(MfaChallengeEntity challenge, MfaMethodEntity method, String code) {
        if (totpService.verify(method.getSecret(), code)) {
            return;
        }

        int failedAttempts = challenge.recordFailedAttempt();
        int failureLimit = mfaSettingsService.current().getChallengeFailureLimit();
        if (failedAttempts >= failureLimit) {
            challenge.markUsed();
            refreshTokenService.revokeAllByUsername(challenge.getUsername(), "MFA_FAILED_LIMIT");
            throw MfaException.badRequest("MFA_FAILED_LIMIT", "MFA verification failed too many times. All sessions were revoked. Please sign in again.");
        }
        int remainingAttempts = failureLimit - failedAttempts;
        throw MfaException.badRequest(
                "MFA_CODE_INVALID",
                "Invalid MFA code. Failed " + failedAttempts + "/" + failureLimit
                        + ". Remaining attempts: " + remainingAttempts + "."
        );
    }

    private boolean policyRequiresUser(MfaPolicy policy, Collection<String> roles) {
        MfaPolicy normalized = policy.normalized();
        if (normalized == MfaPolicy.REQUIRED_FOR_ALL) {
            return true;
        }
        return normalized == MfaPolicy.REQUIRED_FOR_ADMIN
                && roles.stream().anyMatch(role -> "ROLE_ADMIN".equals(role) || "ADMIN".equals(role));
    }

    private String fingerprint(String ipAddress, String userAgent) {
        try {
            String source = (ipAddress == null ? "" : ipAddress) + "|" + (userAgent == null ? "" : userAgent);
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(source.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create MFA request fingerprint.", e);
        }
    }
}

