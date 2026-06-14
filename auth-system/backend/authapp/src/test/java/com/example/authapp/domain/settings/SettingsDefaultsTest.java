package com.example.authapp.domain.settings;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.example.authapp.domain.audit.entity.AuditSettingsEntity;
import com.example.authapp.domain.auth.password.entity.PasswordSettingsEntity;
import com.example.authapp.domain.jwt.entity.TokenSettingsEntity;
import com.example.authapp.domain.risk.entity.RiskDetectionSettingsEntity;
import com.example.authapp.domain.system.settings.entity.RateLimitSettingsEntity;

class SettingsDefaultsTest {

    @Test
    void defaultSettingsExposeSafeIamDefaults() {
        PasswordSettingsEntity password = PasswordSettingsEntity.defaults();
        TokenSettingsEntity token = TokenSettingsEntity.defaults();
        RiskDetectionSettingsEntity risk = RiskDetectionSettingsEntity.defaults();
        AuditSettingsEntity audit = AuditSettingsEntity.defaults();
        RateLimitSettingsEntity rateLimit = RateLimitSettingsEntity.defaults();

        assertThat(password.getMinLength()).isEqualTo(10);
        assertThat(password.isRequireSpecial()).isTrue();
        assertThat(token.getRefreshTokenLifetimeDays()).isEqualTo(7);
        assertThat(token.getRotationGraceSeconds()).isEqualTo(30);
        assertThat(risk.isFirstLoginNewIpExempt()).isTrue();
        assertThat(risk.isTokenReuseRevokeAllSessions()).isTrue();
        assertThat(risk.getCriticalThreshold()).isEqualTo(80);
        assertThat(audit.isAuditLogDeleteDisabled()).isTrue();
        assertThat(rateLimit.getLoginPerIpPerMinute()).isEqualTo(20);
    }
}
