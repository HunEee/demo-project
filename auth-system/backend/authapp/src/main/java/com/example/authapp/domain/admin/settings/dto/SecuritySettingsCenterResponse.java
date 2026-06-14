package com.example.authapp.domain.admin.settings.dto;

import java.util.List;

import com.example.authapp.domain.admin.security.entity.AdminSecuritySettingsEntity;
import com.example.authapp.domain.audit.entity.AuditSettingsEntity;
import com.example.authapp.domain.auth.lockout.entity.AccountLockoutSettingsEntity;
import com.example.authapp.domain.auth.login.entity.LoginSettingsEntity;
import com.example.authapp.domain.auth.password.entity.PasswordSettingsEntity;
import com.example.authapp.domain.auth.verification.entity.VerificationTokenSettingsEntity;
import com.example.authapp.domain.jwt.entity.TokenSettingsEntity;
import com.example.authapp.domain.mfa.entity.MfaPolicy;
import com.example.authapp.domain.mfa.entity.MfaSettingsEntity;
import com.example.authapp.domain.risk.entity.RiskDetectionSettingsEntity;
import com.example.authapp.domain.risk.entity.RiskEventType;
import com.example.authapp.domain.risk.entity.RiskLevel;
import com.example.authapp.domain.risk.entity.RiskRuleSettingsEntity;
import com.example.authapp.domain.session.entity.SessionSettingsEntity;
import com.example.authapp.domain.system.settings.entity.RateLimitSettingsEntity;
import com.example.authapp.domain.system.settings.entity.WebSecuritySettingsEntity;

public record SecuritySettingsCenterResponse(
        AuthenticationSettings authentication,
        SessionTokenSettings sessionToken,
        RiskDetectionSettings riskDetection,
        List<RiskRuleSettings> riskRules,
        OperationalSecuritySettings operationalSecurity
) {
    public record AuthenticationSettings(
            LoginSettings login,
            PasswordSettings password,
            LockoutSettings lockout,
            MfaSecuritySettings mfa,
            VerificationTokenSettings verificationToken
    ) {
        public static AuthenticationSettings from(
                LoginSettingsEntity login,
                PasswordSettingsEntity password,
                AccountLockoutSettingsEntity lockout,
                MfaSettingsEntity mfa,
                VerificationTokenSettingsEntity verificationToken
        ) {
            return new AuthenticationSettings(
                    LoginSettings.from(login),
                    PasswordSettings.from(password),
                    LockoutSettings.from(lockout),
                    MfaSecuritySettings.from(mfa),
                    VerificationTokenSettings.from(verificationToken)
            );
        }
    }

    public record LoginSettings(
            boolean localPasswordLoginEnabled,
            boolean socialLoginEnabled,
            boolean signupEnabled,
            boolean rememberMeEnabled,
            String defaultPostLoginRoute,
            boolean genericLoginFailureMessageEnforced
    ) {
        static LoginSettings from(LoginSettingsEntity entity) {
            return new LoginSettings(entity.isLocalPasswordLoginEnabled(), entity.isSocialLoginEnabled(),
                    entity.isSignupEnabled(), entity.isRememberMeEnabled(), entity.getDefaultPostLoginRoute(),
                    entity.isGenericLoginFailureMessageEnforced());
        }

        public LoginSettingsEntity toEntity() {
            return LoginSettingsEntity.builder()
                    .id(LoginSettingsEntity.SETTINGS_ID)
                    .localPasswordLoginEnabled(localPasswordLoginEnabled)
                    .socialLoginEnabled(socialLoginEnabled)
                    .signupEnabled(signupEnabled)
                    .rememberMeEnabled(rememberMeEnabled)
                    .defaultPostLoginRoute(defaultPostLoginRoute)
                    .genericLoginFailureMessageEnforced(genericLoginFailureMessageEnforced)
                    .build();
        }
    }

    public record PasswordSettings(
            int minLength,
            boolean requireUppercase,
            boolean requireLowercase,
            boolean requireDigit,
            boolean requireSpecial,
            int historyCount,
            int expirationDays,
            int temporaryPasswordExpirationMinutes,
            boolean blockUsernameEmailInclusion
    ) {
        static PasswordSettings from(PasswordSettingsEntity entity) {
            return new PasswordSettings(entity.getMinLength(), entity.isRequireUppercase(), entity.isRequireLowercase(),
                    entity.isRequireDigit(), entity.isRequireSpecial(), entity.getHistoryCount(), entity.getExpirationDays(),
                    entity.getTemporaryPasswordExpirationMinutes(), entity.isBlockUsernameEmailInclusion());
        }

        public PasswordSettingsEntity toEntity() {
            return PasswordSettingsEntity.builder()
                    .id(PasswordSettingsEntity.SETTINGS_ID)
                    .minLength(minLength)
                    .requireUppercase(requireUppercase)
                    .requireLowercase(requireLowercase)
                    .requireDigit(requireDigit)
                    .requireSpecial(requireSpecial)
                    .historyCount(historyCount)
                    .expirationDays(expirationDays)
                    .temporaryPasswordExpirationMinutes(temporaryPasswordExpirationMinutes)
                    .blockUsernameEmailInclusion(blockUsernameEmailInclusion)
                    .build();
        }
    }

    public record LockoutSettings(
            int failedLoginThreshold,
            int failureWindowMinutes,
            int lockDurationMinutes,
            int adminFailedLoginThreshold,
            boolean autoUnlockEnabled,
            boolean manualUnlockAuditRequired
    ) {
        static LockoutSettings from(AccountLockoutSettingsEntity entity) {
            return new LockoutSettings(entity.getFailedLoginThreshold(), entity.getFailureWindowMinutes(),
                    entity.getLockDurationMinutes(), entity.getAdminFailedLoginThreshold(),
                    entity.isAutoUnlockEnabled(), entity.isManualUnlockAuditRequired());
        }

        public AccountLockoutSettingsEntity toEntity() {
            return AccountLockoutSettingsEntity.builder()
                    .id(AccountLockoutSettingsEntity.SETTINGS_ID)
                    .failedLoginThreshold(failedLoginThreshold)
                    .failureWindowMinutes(failureWindowMinutes)
                    .lockDurationMinutes(lockDurationMinutes)
                    .adminFailedLoginThreshold(adminFailedLoginThreshold)
                    .autoUnlockEnabled(autoUnlockEnabled)
                    .manualUnlockAuditRequired(manualUnlockAuditRequired)
                    .build();
        }
    }

    public record MfaSecuritySettings(
            MfaPolicy policy,
            int highRiskMfaThreshold,
            int temporaryExceptionMaxDays,
            int challengeFailureLimit,
            int challengeExpirationMinutes
    ) {
        static MfaSecuritySettings from(MfaSettingsEntity entity) {
            return new MfaSecuritySettings(entity.getPolicy(), entity.getHighRiskMfaThreshold(),
                    entity.getTemporaryExceptionMaxDays(), entity.getChallengeFailureLimit(),
                    entity.getChallengeExpirationMinutes());
        }

        public MfaSettingsEntity toEntity() {
            return MfaSettingsEntity.builder()
                    .id(MfaSettingsEntity.SETTINGS_ID)
                    .policy(policy == null ? MfaPolicy.OPTIONAL : policy.normalized())
                    .highRiskMfaThreshold(highRiskMfaThreshold)
                    .temporaryExceptionMaxDays(temporaryExceptionMaxDays)
                    .challengeFailureLimit(challengeFailureLimit)
                    .challengeExpirationMinutes(challengeExpirationMinutes)
                    .build();
        }
    }

    public record VerificationTokenSettings(
            int signupTokenExpirationMinutes,
            int passwordResetTokenExpirationMinutes,
            int maxVerificationAttempts,
            int resendCooldownSeconds,
            int dailySendLimitPerEmail,
            boolean invalidatePreviousTokenOnResend
    ) {
        static VerificationTokenSettings from(VerificationTokenSettingsEntity entity) {
            return new VerificationTokenSettings(entity.getSignupTokenExpirationMinutes(),
                    entity.getPasswordResetTokenExpirationMinutes(), entity.getMaxVerificationAttempts(),
                    entity.getResendCooldownSeconds(), entity.getDailySendLimitPerEmail(),
                    entity.isInvalidatePreviousTokenOnResend());
        }

        public VerificationTokenSettingsEntity toEntity() {
            return VerificationTokenSettingsEntity.builder()
                    .id(VerificationTokenSettingsEntity.SETTINGS_ID)
                    .signupTokenExpirationMinutes(signupTokenExpirationMinutes)
                    .passwordResetTokenExpirationMinutes(passwordResetTokenExpirationMinutes)
                    .maxVerificationAttempts(maxVerificationAttempts)
                    .resendCooldownSeconds(resendCooldownSeconds)
                    .dailySendLimitPerEmail(dailySendLimitPerEmail)
                    .invalidatePreviousTokenOnResend(invalidatePreviousTokenOnResend)
                    .build();
        }
    }

    public record SessionTokenSettings(
            int accessTokenLifetimeMinutes,
            int refreshTokenLifetimeDays,
            boolean rotationEnabled,
            int rotationGraceSeconds,
            boolean allowRecentRotationRecovery,
            int maxActiveSessionsUser,
            int maxActiveSessionsAdmin,
            int idleTimeoutMinutes,
            boolean revokeOnPasswordChange,
            boolean revokeOnMfaReset
    ) {
        public static SessionTokenSettings from(TokenSettingsEntity token, SessionSettingsEntity session) {
            return new SessionTokenSettings(token.getAccessTokenLifetimeMinutes(), token.getRefreshTokenLifetimeDays(),
                    token.isRotationEnabled(), token.getRotationGraceSeconds(), token.isAllowRecentRotationRecovery(),
                    session.getMaxActiveSessionsUser(), session.getMaxActiveSessionsAdmin(), session.getIdleTimeoutMinutes(),
                    session.isRevokeOnPasswordChange(), session.isRevokeOnMfaReset());
        }

        public TokenSettingsEntity toTokenEntity() {
            return TokenSettingsEntity.builder()
                    .id(TokenSettingsEntity.SETTINGS_ID)
                    .accessTokenLifetimeMinutes(accessTokenLifetimeMinutes)
                    .refreshTokenLifetimeDays(refreshTokenLifetimeDays)
                    .rotationEnabled(rotationEnabled)
                    .rotationGraceSeconds(rotationGraceSeconds)
                    .allowRecentRotationRecovery(allowRecentRotationRecovery)
                    .build();
        }

        public SessionSettingsEntity toSessionEntity() {
            return SessionSettingsEntity.builder()
                    .id(SessionSettingsEntity.SETTINGS_ID)
                    .maxActiveSessionsUser(maxActiveSessionsUser)
                    .maxActiveSessionsAdmin(maxActiveSessionsAdmin)
                    .idleTimeoutMinutes(idleTimeoutMinutes)
                    .revokeOnPasswordChange(revokeOnPasswordChange)
                    .revokeOnMfaReset(revokeOnMfaReset)
                    .build();
        }
    }

    public record RiskDetectionSettings(
            int mediumThreshold,
            int highThreshold,
            int criticalThreshold,
            int mfaRequiredScore,
            int tokenRevokeScore,
            int loginBlockScore,
            int revokeAllSessionsScore,
            boolean autoResponseEnabled,
            boolean firstLoginNewIpExempt,
            boolean firstLoginNewUserAgentExempt,
            boolean tokenReuseForceCritical,
            boolean tokenReuseRevokeAllSessions,
            boolean tokenContextChangeRevokeFamily
    ) {
        public static RiskDetectionSettings from(RiskDetectionSettingsEntity entity) {
            return new RiskDetectionSettings(entity.getMediumThreshold(), entity.getHighThreshold(), entity.getCriticalThreshold(),
                    entity.getMfaRequiredScore(), entity.getTokenRevokeScore(), entity.getLoginBlockScore(),
                    entity.getRevokeAllSessionsScore(), entity.isAutoResponseEnabled(), entity.isFirstLoginNewIpExempt(),
                    entity.isFirstLoginNewUserAgentExempt(), entity.isTokenReuseForceCritical(),
                    entity.isTokenReuseRevokeAllSessions(), entity.isTokenContextChangeRevokeFamily());
        }

        public RiskDetectionSettingsEntity toEntity() {
            return RiskDetectionSettingsEntity.builder()
                    .id(RiskDetectionSettingsEntity.SETTINGS_ID)
                    .mediumThreshold(mediumThreshold)
                    .highThreshold(highThreshold)
                    .criticalThreshold(criticalThreshold)
                    .mfaRequiredScore(mfaRequiredScore)
                    .tokenRevokeScore(tokenRevokeScore)
                    .loginBlockScore(loginBlockScore)
                    .revokeAllSessionsScore(revokeAllSessionsScore)
                    .autoResponseEnabled(autoResponseEnabled)
                    .firstLoginNewIpExempt(firstLoginNewIpExempt)
                    .firstLoginNewUserAgentExempt(firstLoginNewUserAgentExempt)
                    .tokenReuseForceCritical(tokenReuseForceCritical)
                    .tokenReuseRevokeAllSessions(tokenReuseRevokeAllSessions)
                    .tokenContextChangeRevokeFamily(tokenContextChangeRevokeFamily)
                    .build();
        }
    }

    public record RiskRuleSettings(
            Long id,
            RiskEventType eventType,
            boolean enabled,
            int score,
            RiskLevel riskLevel,
            Integer thresholdCount,
            Integer windowMinutes,
            Integer nightStartHour,
            Integer nightEndHour,
            String description
    ) {
        public static RiskRuleSettings from(RiskRuleSettingsEntity entity) {
            return new RiskRuleSettings(entity.getId(), entity.getEventType(), entity.isEnabled(), entity.getScore(),
                    entity.getRiskLevel(), entity.getThresholdCount(), entity.getWindowMinutes(), entity.getNightStartHour(),
                    entity.getNightEndHour(), entity.getDescription());
        }

        public RiskRuleSettingsEntity toEntity() {
            return RiskRuleSettingsEntity.builder()
                    .id(id)
                    .eventType(eventType)
                    .enabled(enabled)
                    .score(score)
                    .riskLevel(riskLevel)
                    .thresholdCount(thresholdCount)
                    .windowMinutes(windowMinutes)
                    .nightStartHour(nightStartHour)
                    .nightEndHour(nightEndHour)
                    .description(description)
                    .build();
        }
    }

    public record OperationalSecuritySettings(
            AdminAccessSettings adminAccess,
            CorsRedirectSettings corsRedirect,
            LogRetentionSettings logRetention,
            RateLimitSettings rateLimit
    ) {
        public static OperationalSecuritySettings from(
                AdminSecuritySettingsEntity admin,
                WebSecuritySettingsEntity web,
                AuditSettingsEntity audit,
                RateLimitSettingsEntity rate
        ) {
            return new OperationalSecuritySettings(
                    AdminAccessSettings.from(admin),
                    CorsRedirectSettings.from(web),
                    LogRetentionSettings.from(audit),
                    RateLimitSettings.from(rate)
            );
        }
    }

    public record AdminAccessSettings(
            boolean adminIpAllowlistEnabled,
            boolean denyUnknownProxyHeaders,
            String trustedProxyHeaderMode,
            boolean requireMfaForAdminAccess,
            int adminSessionMaxAgeMinutes,
            String allowedAdminIps
    ) {
        static AdminAccessSettings from(AdminSecuritySettingsEntity entity) {
            return new AdminAccessSettings(entity.isAdminIpAllowlistEnabled(), entity.isDenyUnknownProxyHeaders(),
                    entity.getTrustedProxyHeaderMode(), entity.isRequireMfaForAdminAccess(),
                    entity.getAdminSessionMaxAgeMinutes(), entity.getAllowedAdminIps());
        }

        public AdminSecuritySettingsEntity toEntity() {
            return AdminSecuritySettingsEntity.builder()
                    .id(AdminSecuritySettingsEntity.SETTINGS_ID)
                    .adminIpAllowlistEnabled(adminIpAllowlistEnabled)
                    .denyUnknownProxyHeaders(denyUnknownProxyHeaders)
                    .trustedProxyHeaderMode(trustedProxyHeaderMode)
                    .requireMfaForAdminAccess(requireMfaForAdminAccess)
                    .adminSessionMaxAgeMinutes(adminSessionMaxAgeMinutes)
                    .allowedAdminIps(allowedAdminIps)
                    .build();
        }
    }

    public record CorsRedirectSettings(
            boolean allowCredentials,
            String allowedMethods,
            String allowedOrigins,
            String allowedRedirectUris
    ) {
        static CorsRedirectSettings from(WebSecuritySettingsEntity entity) {
            return new CorsRedirectSettings(entity.isAllowCredentials(), entity.getAllowedMethods(),
                    entity.getAllowedOrigins(), entity.getAllowedRedirectUris());
        }

        public WebSecuritySettingsEntity toEntity() {
            return WebSecuritySettingsEntity.builder()
                    .id(WebSecuritySettingsEntity.SETTINGS_ID)
                    .allowCredentials(allowCredentials)
                    .allowedMethods(allowedMethods)
                    .allowedOrigins(allowedOrigins)
                    .allowedRedirectUris(allowedRedirectUris)
                    .build();
        }
    }

    public record LogRetentionSettings(
            int auditLogRetentionDays,
            int loginHistoryRetentionDays,
            int riskEventRetentionDays,
            int securityIncidentRetentionDays,
            int adminActionLogRetentionDays,
            boolean exportRequiresReason,
            boolean auditLogDeleteDisabled,
            boolean archiveBeforePurge
    ) {
        static LogRetentionSettings from(AuditSettingsEntity entity) {
            return new LogRetentionSettings(entity.getAuditLogRetentionDays(), entity.getLoginHistoryRetentionDays(),
                    entity.getRiskEventRetentionDays(), entity.getSecurityIncidentRetentionDays(),
                    entity.getAdminActionLogRetentionDays(), entity.isExportRequiresReason(),
                    entity.isAuditLogDeleteDisabled(), entity.isArchiveBeforePurge());
        }

        public AuditSettingsEntity toEntity() {
            return AuditSettingsEntity.builder()
                    .id(AuditSettingsEntity.SETTINGS_ID)
                    .auditLogRetentionDays(auditLogRetentionDays)
                    .loginHistoryRetentionDays(loginHistoryRetentionDays)
                    .riskEventRetentionDays(riskEventRetentionDays)
                    .securityIncidentRetentionDays(securityIncidentRetentionDays)
                    .adminActionLogRetentionDays(adminActionLogRetentionDays)
                    .exportRequiresReason(exportRequiresReason)
                    .auditLogDeleteDisabled(auditLogDeleteDisabled)
                    .archiveBeforePurge(archiveBeforePurge)
                    .build();
        }
    }

    public record RateLimitSettings(
            int loginPerIpPerMinute,
            int loginPerUsernamePerMinute,
            int refreshPerSessionPerMinute,
            int passwordResetPerEmailPerDay,
            int verificationEmailPerEmailPerDay,
            int adminWritePerMinute
    ) {
        static RateLimitSettings from(RateLimitSettingsEntity entity) {
            return new RateLimitSettings(entity.getLoginPerIpPerMinute(), entity.getLoginPerUsernamePerMinute(),
                    entity.getRefreshPerSessionPerMinute(), entity.getPasswordResetPerEmailPerDay(),
                    entity.getVerificationEmailPerEmailPerDay(), entity.getAdminWritePerMinute());
        }

        public RateLimitSettingsEntity toEntity() {
            return RateLimitSettingsEntity.builder()
                    .id(RateLimitSettingsEntity.SETTINGS_ID)
                    .loginPerIpPerMinute(loginPerIpPerMinute)
                    .loginPerUsernamePerMinute(loginPerUsernamePerMinute)
                    .refreshPerSessionPerMinute(refreshPerSessionPerMinute)
                    .passwordResetPerEmailPerDay(passwordResetPerEmailPerDay)
                    .verificationEmailPerEmailPerDay(verificationEmailPerEmailPerDay)
                    .adminWritePerMinute(adminWritePerMinute)
                    .build();
        }
    }
}
