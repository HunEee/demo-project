package com.example.authapp.domain.mfa.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.authapp.domain.admin.AdminSettingsStore;
import com.example.authapp.domain.jwt.service.RefreshTokenService;
import com.example.authapp.domain.mfa.dto.MfaVerifyRequest;
import com.example.authapp.domain.mfa.entity.MfaChallengeEntity;
import com.example.authapp.domain.mfa.entity.MfaExceptionEntity;
import com.example.authapp.domain.mfa.entity.MfaMethodEntity;
import com.example.authapp.domain.mfa.entity.MfaMethodType;
import com.example.authapp.domain.mfa.entity.MfaPolicy;
import com.example.authapp.domain.mfa.repository.MfaChallengeRepository;
import com.example.authapp.domain.mfa.repository.MfaExceptionRepository;
import com.example.authapp.domain.mfa.repository.MfaMethodRepository;

@ExtendWith(MockitoExtension.class)
class MfaServiceTest {

    @Mock
    private MfaMethodRepository mfaMethodRepository;

    @Mock
    private MfaChallengeRepository mfaChallengeRepository;

    @Mock
    private MfaExceptionRepository mfaExceptionRepository;

    @Mock
    private AdminSettingsStore adminSettingsStore;

    @Mock
    private TotpService totpService;

    @Mock
    private RefreshTokenService refreshTokenService;

    private MfaService service() {
        return new MfaService(
                mfaMethodRepository,
                mfaChallengeRepository,
                mfaExceptionRepository,
                adminSettingsStore,
                totpService,
                refreshTokenService
        );
    }

    @Test
    void adminPolicyRequiresMfaForAdminWithRegisteredTotp() {
        when(mfaExceptionRepository.findActiveByUsername("admin")).thenReturn(Optional.empty());
        when(mfaMethodRepository.findByUsernameAndEnabledTrue("admin"))
                .thenReturn(List.of(MfaMethodEntity.builder()
                        .username("admin")
                        .type(MfaMethodType.TOTP)
                        .enabled(true)
                        .build()));

        var decision = service().evaluateLogin("admin", List.of("ROLE_ADMIN"));

        assertThat(decision.required()).isTrue();
        assertThat(decision.registrationRequired()).isFalse();
        assertThat(decision.availableMethods()).containsExactly(MfaMethodType.TOTP);
    }

    @Test
    void registeredTotpRequiresMfaEvenWhenPolicyIsOff() {
        when(mfaExceptionRepository.findActiveByUsername("user1")).thenReturn(Optional.empty());
        when(mfaMethodRepository.findByUsernameAndEnabledTrue("user1"))
                .thenReturn(List.of(MfaMethodEntity.builder()
                        .username("user1")
                        .type(MfaMethodType.TOTP)
                        .enabled(true)
                        .build()));

        var decision = service().evaluateLogin("user1", List.of("ROLE_USER"));

        assertThat(decision.required()).isTrue();
        assertThat(decision.registrationRequired()).isFalse();
        assertThat(decision.availableMethods()).containsExactly(MfaMethodType.TOTP);
    }

    @Test
    void activeExceptionBypassesRequiredPolicy() {
        when(mfaExceptionRepository.findActiveByUsername("user1"))
                .thenReturn(Optional.of(MfaExceptionEntity.builder().username("user1").build()));

        var decision = service().evaluateLogin("user1", List.of("ROLE_USER"));

        assertThat(decision.required()).isFalse();
        assertThat(decision.registrationRequired()).isFalse();
    }

    @Test
    void resetDisablesRegisteredMethodsAndExpiresChallenges() {
        service().resetUserMfa("user1");

        verify(mfaMethodRepository).deleteByUsername("user1");
        verify(mfaChallengeRepository).expireOpenChallenges("user1");
    }

    @Test
    void setupTotpClearsPendingMethodsForSameUserBeforeCreatingNewSecret() {
        when(totpService.generateSecret()).thenReturn("SECRET");
        when(mfaMethodRepository.save(any(MfaMethodEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(totpService.otpAuthUri("admin", "SECRET")).thenReturn("otpauth://totp/AuthApp:admin?secret=SECRET&issuer=AuthApp");
        when(totpService.qrCodeDataUri("otpauth://totp/AuthApp:admin?secret=SECRET&issuer=AuthApp")).thenReturn("data:image/png;base64,QR");

        var response = service().setupTotp("admin");

        verify(mfaMethodRepository).deleteByUsernameAndTypeAndEnabledFalse("admin", MfaMethodType.TOTP);
        assertThat(response.otpAuthUri()).contains("AuthApp:admin");
    }

    @Test
    void verifyChallengeLocksChallengeAfterRepeatedInvalidCodes() {
        MfaChallengeEntity challenge = MfaChallengeEntity.builder()
                .challengeId("challenge-1")
                .username("user1")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .used(false)
                .verified(false)
                .build();
        MfaMethodEntity method = MfaMethodEntity.builder()
                .username("user1")
                .type(MfaMethodType.TOTP)
                .enabled(true)
                .secret("SECRET")
                .build();
        when(mfaChallengeRepository.findByChallengeIdAndUsedFalse("challenge-1")).thenReturn(Optional.of(challenge));
        when(mfaMethodRepository.findByUsernameAndTypeAndEnabledTrue("user1", MfaMethodType.TOTP)).thenReturn(Optional.of(method));
        when(totpService.verify("SECRET", "000000")).thenReturn(false);

        MfaService service = service();
        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> service.verifyChallenge(new MfaVerifyRequest("challenge-1", MfaMethodType.TOTP, "000000")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("실패 " + (i + 1) + "/5회")
                    .hasMessageContaining("남은 시도 " + (4 - i) + "회");
        }

        assertThatThrownBy(() -> service.verifyChallenge(new MfaVerifyRequest("challenge-1", MfaMethodType.TOTP, "000000")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("5회 실패")
                .hasMessageContaining("모든 세션이 로그아웃");
        assertThat(challenge.isUsed()).isTrue();
        verify(refreshTokenService).revokeAllByUsername("user1", "MFA_FAILED_LIMIT");
    }
}
