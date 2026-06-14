package com.example.authapp.domain.settings;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.example.authapp.domain.admin.security.entity.AdminSecuritySettingsEntity;
import com.example.authapp.domain.audit.entity.AuditSettingsEntity;
import com.example.authapp.domain.auth.lockout.entity.AccountLockoutSettingsEntity;
import com.example.authapp.domain.auth.login.entity.LoginSettingsEntity;
import com.example.authapp.domain.auth.password.entity.PasswordSettingsEntity;
import com.example.authapp.domain.auth.verification.entity.VerificationTokenSettingsEntity;
import com.example.authapp.domain.jwt.entity.TokenSettingsEntity;
import com.example.authapp.domain.mfa.entity.MfaSettingsEntity;
import com.example.authapp.domain.risk.entity.RiskDetectionSettingsEntity;
import com.example.authapp.domain.risk.entity.RiskRuleSettingsEntity;
import com.example.authapp.domain.session.entity.SessionSettingsEntity;
import com.example.authapp.domain.system.settings.entity.RateLimitSettingsEntity;
import com.example.authapp.domain.system.settings.entity.WebSecuritySettingsEntity;

import jakarta.persistence.Table;

class SettingsEntityTableNameTest {

    @Test
    void settingsEntitiesUseOwningDomainTableNames() {
        assertThat(tableName(LoginSettingsEntity.class)).isEqualTo("login_settings");
        assertThat(tableName(PasswordSettingsEntity.class)).isEqualTo("password_settings");
        assertThat(tableName(AccountLockoutSettingsEntity.class)).isEqualTo("account_lockout_settings");
        assertThat(tableName(VerificationTokenSettingsEntity.class)).isEqualTo("verification_token_settings");
        assertThat(tableName(MfaSettingsEntity.class)).isEqualTo("mfa_settings");
        assertThat(tableName(TokenSettingsEntity.class)).isEqualTo("token_settings");
        assertThat(tableName(SessionSettingsEntity.class)).isEqualTo("session_settings");
        assertThat(tableName(RiskDetectionSettingsEntity.class)).isEqualTo("risk_detection_settings");
        assertThat(tableName(RiskRuleSettingsEntity.class)).isEqualTo("risk_rule_settings");
        assertThat(tableName(AdminSecuritySettingsEntity.class)).isEqualTo("admin_security_settings");
        assertThat(tableName(WebSecuritySettingsEntity.class)).isEqualTo("web_security_settings");
        assertThat(tableName(AuditSettingsEntity.class)).isEqualTo("audit_settings");
        assertThat(tableName(RateLimitSettingsEntity.class)).isEqualTo("rate_limit_settings");
    }

    private String tableName(Class<?> type) {
        return type.getAnnotation(Table.class).name();
    }
}
