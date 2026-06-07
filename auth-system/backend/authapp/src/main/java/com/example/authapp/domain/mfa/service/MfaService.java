package com.example.authapp.domain.mfa.service;

import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
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
import com.example.authapp.domain.mfa.dto.TotpConfirmRequest;
import com.example.authapp.domain.mfa.dto.TotpSetupResponse;
import com.example.authapp.domain.mfa.entity.MfaChallengeEntity;
import com.example.authapp.domain.mfa.entity.MfaMethodEntity;
import com.example.authapp.domain.mfa.entity.MfaMethodType;
import com.example.authapp.domain.mfa.entity.MfaPolicy;
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

    @Transactional(readOnly = true)
    public MfaLoginDecision evaluateLogin(String username, Collection<String> roles) {
        if (mfaExceptionRepository.findActiveByUsername(username).isPresent()) {
            return MfaLoginDecision.notRequired();
        }

        List<MfaMethodEntity> methods = mfaMethodRepository.findByUsernameAndEnabledTrue(username);
        if (!methods.isEmpty()) {
            return new MfaLoginDecision(
                    true,
                    false,
                    methods.stream().map(MfaMethodEntity::getType).distinct().toList()
            );
        }

        MfaPolicy policy = adminSettingsStore.mfaPolicy();
        if (policy.normalized() == MfaPolicy.OPTIONAL || !policyRequiresUser(policy, roles)) {
            return MfaLoginDecision.notRequired();
        }
        return MfaLoginDecision.requireRegistration();
    }

    public void resetUserMfa(String username) {
        mfaMethodRepository.deleteByUsername(username);
        mfaChallengeRepository.expireOpenChallenges(username);
    }

    public TotpSetupResponse setupTotp(String username) {
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

    public MfaMethodResponse confirmTotp(String username, TotpConfirmRequest request) {
        MfaMethodEntity method = mfaMethodRepository.findById(request.methodId())
                .filter(item -> item.getUsername().equals(username))
                .filter(item -> item.getType() == MfaMethodType.TOTP)
                .orElseThrow(() -> new IllegalArgumentException("MFA method not found."));
        if (method.isEnabled()) {
            throw new IllegalStateException("MFA method is already registered.");
        }
        if (!totpService.verify(method.getSecret(), request.code())) {
            throw new IllegalArgumentException("Invalid MFA code.");
        }
        mfaMethodRepository.deleteActiveByUsernameAndType(username, MfaMethodType.TOTP);
        method.confirmRegistration();
        return MfaMethodResponse.from(method);
    }

    @Transactional(readOnly = true)
    public List<MfaMethodResponse> methods(String username) {
        return mfaMethodRepository.findByUsername(username)
                .stream()
                .map(MfaMethodResponse::from)
                .toList();
    }

    public void deleteMethod(String username, Long id) {
        MfaMethodEntity method = mfaMethodRepository.findById(id)
                .filter(item -> item.getUsername().equals(username))
                .orElseThrow(() -> new IllegalArgumentException("MFA method not found."));
        mfaMethodRepository.delete(method);
    }

    public MfaChallengeResult createChallenge(String username, MfaLoginDecision decision, String ipAddress, String userAgent) {
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
        return new MfaChallengeResult(challengeId, expiresAt, decision.registrationRequired(), decision.availableMethods());
    }

    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public String verifyChallenge(MfaVerifyRequest request) {
        return verifyChallenge(request, null, null);
    }

    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public String verifyChallenge(MfaVerifyRequest request, String ipAddress, String userAgent) {
        MfaChallengeEntity challenge = mfaChallengeRepository.findByChallengeIdAndUsedFalse(request.challengeId())
                .orElseThrow(() -> new IllegalArgumentException("MFA challenge not found."));
        if (challenge.isExpired()) {
            challenge.markUsed();
            throw new IllegalArgumentException("MFA challenge expired.");
        }
        if (ipAddress != null && userAgent != null && !fingerprint(ipAddress, userAgent).equals(challenge.getRequestFingerprint())) {
            challenge.markUsed();
            throw new IllegalArgumentException("MFA challenge verification context changed. Please sign in again.");
        }
        if (request.methodType() != MfaMethodType.TOTP) {
            throw new IllegalArgumentException("Unsupported MFA method.");
        }
        MfaMethodEntity method = mfaMethodRepository.findByUsernameAndTypeAndEnabledTrue(challenge.getUsername(), MfaMethodType.TOTP)
                .orElseThrow(() -> new IllegalArgumentException("TOTP is not registered."));
        if (!totpService.verify(method.getSecret(), request.code())) {
            int failedAttempts = challenge.recordFailedAttempt();
            if (failedAttempts >= MAX_CHALLENGE_FAILURES) {
                challenge.markUsed();
                refreshTokenService.revokeAllByUsername(challenge.getUsername());
                throw new IllegalArgumentException("MFA 인증 5회 실패로 모든 세션이 로그아웃되었습니다. 다시 로그인해주세요.");
            }
            int remainingAttempts = MAX_CHALLENGE_FAILURES - failedAttempts;
            throw new IllegalArgumentException("MFA 인증 코드가 올바르지 않습니다. 실패 " + failedAttempts + "/"
                    + MAX_CHALLENGE_FAILURES + "회, 남은 시도 " + remainingAttempts + "회");
        }
        method.markUsed();
        challenge.markVerifiedAndUsed();
        return challenge.getUsername();
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
