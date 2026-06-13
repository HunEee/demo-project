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
import com.example.authapp.domain.mfa.exception.MfaException;
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
    void activeExceptionBypassesRequiredPolicyWhenNoMethodIsRegistered() {
        when(mfaMethodRepository.findByUsernameAndEnabledTrue("user1")).thenReturn(List.of());
        when(mfaExceptionRepository.findActiveByUsername("user1"))
                .thenReturn(Optional.of(MfaExceptionEntity.builder().username("user1").build()));

        var decision = service().evaluateLogin("user1", List.of("ROLE_USER"));

        assertThat(decision.required()).isFalse();
        assertThat(decision.registrationRequired()).isFalse();
    }

    @Test
    void activeExceptionDoesNotBypassAlreadyRegisteredTotp() {
        when(mfaMethodRepository.findByUsernameAndEnabledTrue("user1"))
                .thenReturn(List.of(MfaMethodEntity.builder()
                        .username("user1")
                        .type(MfaMethodType.TOTP)
                        .enabled(true)
                        .build()));

        var decision = service().evaluateLogin("user1", List.of("ROLE_USER"));

        assertThat(decision.required()).isTrue();
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
    void deleteMethodRequiresCurrentTotpCode() {
        MfaMethodEntity method = MfaMethodEntity.builder()
                .id(10L)
                .username("user1")
                .type(MfaMethodType.TOTP)
                .enabled(true)
                .secret("SECRET")
                .build();
        when(mfaMethodRepository.findById(10L)).thenReturn(Optional.of(method));
        when(totpService.verify("SECRET", "111111")).thenReturn(false);

        assertThatThrownBy(() -> service().deleteMethod("user1", 10L, "111111"))
                .isInstanceOf(MfaException.class)
                .hasMessageContaining("Invalid MFA code");
    }
}
