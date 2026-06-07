package com.example.authapp.domain.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.authapp.domain.mfa.entity.MfaPolicy;

@ExtendWith(MockitoExtension.class)
class AdminSettingsStoreTest {

    @Mock
    private AdminSettingsRepository repository;

    @Test
    void mfaPolicyReadsPersistedSettingInsteadOfResettingToOff() {
        when(repository.findById(1L)).thenReturn(Optional.of(AdminSettingsEntity.builder()
                .id(1L)
                .maxLoginFailures(5)
                .highRiskThreshold(60)
                .criticalRiskThreshold(80)
                .sessionExpireDays(14)
                .forceLogoutOnCriticalRisk(true)
                .mfaPolicy(MfaPolicy.REQUIRED_FOR_ADMIN)
                .build()));

        assertThat(new AdminSettingsStore(repository).mfaPolicy()).isEqualTo(MfaPolicy.REQUIRED_FOR_ADMIN);
    }
}
