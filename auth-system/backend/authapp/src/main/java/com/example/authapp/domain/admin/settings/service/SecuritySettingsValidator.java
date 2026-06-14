package com.example.authapp.domain.admin.settings.service;

import com.example.authapp.domain.admin.settings.dto.SecuritySettingsCenterResponse;
import com.example.authapp.domain.admin.settings.dto.SecuritySettingsUpdateRequest;

public final class SecuritySettingsValidator {

    private SecuritySettingsValidator() {
    }

    public static void validate(SecuritySettingsUpdateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Security settings request is required.");
        }
        var authentication = request.authentication();
        var sessionToken = request.sessionToken();
        var riskDetection = request.riskDetection();
        var operational = request.operationalSecurity();
        requirePresent("authentication", authentication);
        requirePresent("sessionToken", sessionToken);
        requirePresent("riskDetection", riskDetection);
        requirePresent("operationalSecurity", operational);

        validateRiskThresholds(riskDetection.mediumThreshold(), riskDetection.highThreshold(), riskDetection.criticalThreshold());
        requireScore("riskDetection.mfaRequiredScore", riskDetection.mfaRequiredScore());
        requireScore("riskDetection.tokenRevokeScore", riskDetection.tokenRevokeScore());
        requireScore("riskDetection.loginBlockScore", riskDetection.loginBlockScore());
        requireScore("riskDetection.revokeAllSessionsScore", riskDetection.revokeAllSessionsScore());

        requirePositive("authentication.password.minLength", authentication.password().minLength());
        requirePositive("authentication.lockout.failedLoginThreshold", authentication.lockout().failedLoginThreshold());
        requirePositive("authentication.lockout.failureWindowMinutes", authentication.lockout().failureWindowMinutes());
        requirePositive("authentication.lockout.lockDurationMinutes", authentication.lockout().lockDurationMinutes());
        requirePositive("authentication.lockout.adminFailedLoginThreshold", authentication.lockout().adminFailedLoginThreshold());
        requireScore("authentication.mfa.highRiskMfaThreshold", authentication.mfa().highRiskMfaThreshold());
        requirePositive("authentication.mfa.temporaryExceptionMaxDays", authentication.mfa().temporaryExceptionMaxDays());
        requirePositive("authentication.mfa.challengeFailureLimit", authentication.mfa().challengeFailureLimit());
        requirePositive("authentication.mfa.challengeExpirationMinutes", authentication.mfa().challengeExpirationMinutes());
        requirePositive("authentication.verificationToken.signupTokenExpirationMinutes", authentication.verificationToken().signupTokenExpirationMinutes());
        requirePositive("authentication.verificationToken.passwordResetTokenExpirationMinutes", authentication.verificationToken().passwordResetTokenExpirationMinutes());
        requirePositive("authentication.verificationToken.maxVerificationAttempts", authentication.verificationToken().maxVerificationAttempts());
        requirePositive("authentication.verificationToken.resendCooldownSeconds", authentication.verificationToken().resendCooldownSeconds());
        requirePositive("authentication.verificationToken.dailySendLimitPerEmail", authentication.verificationToken().dailySendLimitPerEmail());

        requirePositive("sessionToken.accessTokenLifetimeMinutes", sessionToken.accessTokenLifetimeMinutes());
        requirePositive("sessionToken.refreshTokenLifetimeDays", sessionToken.refreshTokenLifetimeDays());
        requirePositive("sessionToken.rotationGraceSeconds", sessionToken.rotationGraceSeconds());
        requirePositive("sessionToken.maxActiveSessionsUser", sessionToken.maxActiveSessionsUser());
        requirePositive("sessionToken.maxActiveSessionsAdmin", sessionToken.maxActiveSessionsAdmin());
        requirePositive("sessionToken.idleTimeoutMinutes", sessionToken.idleTimeoutMinutes());

        validateCorsOrigins(operational.corsRedirect().allowedOrigins(), operational.corsRedirect().allowCredentials());
        requirePositive("operationalSecurity.adminAccess.adminSessionMaxAgeMinutes", operational.adminAccess().adminSessionMaxAgeMinutes());
        requirePositive("operationalSecurity.logRetention.auditLogRetentionDays", operational.logRetention().auditLogRetentionDays());
        requirePositive("operationalSecurity.logRetention.loginHistoryRetentionDays", operational.logRetention().loginHistoryRetentionDays());
        requirePositive("operationalSecurity.logRetention.riskEventRetentionDays", operational.logRetention().riskEventRetentionDays());
        requirePositive("operationalSecurity.logRetention.securityIncidentRetentionDays", operational.logRetention().securityIncidentRetentionDays());
        requirePositive("operationalSecurity.logRetention.adminActionLogRetentionDays", operational.logRetention().adminActionLogRetentionDays());
        requirePositive("operationalSecurity.rateLimit.loginPerIpPerMinute", operational.rateLimit().loginPerIpPerMinute());
        requirePositive("operationalSecurity.rateLimit.loginPerUsernamePerMinute", operational.rateLimit().loginPerUsernamePerMinute());
        requirePositive("operationalSecurity.rateLimit.refreshPerSessionPerMinute", operational.rateLimit().refreshPerSessionPerMinute());
        requirePositive("operationalSecurity.rateLimit.passwordResetPerEmailPerDay", operational.rateLimit().passwordResetPerEmailPerDay());
        requirePositive("operationalSecurity.rateLimit.verificationEmailPerEmailPerDay", operational.rateLimit().verificationEmailPerEmailPerDay());
        requirePositive("operationalSecurity.rateLimit.adminWritePerMinute", operational.rateLimit().adminWritePerMinute());

        if (request.riskRules() != null) {
            request.riskRules().forEach(SecuritySettingsValidator::validateRiskRule);
        }
    }

    public static void validateRiskThresholds(int medium, int high, int critical) {
        requireScore("medium", medium);
        requireScore("high", high);
        requireScore("critical", critical);
        if (!(medium < high && high < critical)) {
            throw new IllegalArgumentException("Risk thresholds must satisfy medium < high < critical.");
        }
    }

    public static void validateCorsOrigin(String origin, boolean allowCredentials) {
        if (allowCredentials && "*".equals(origin)) {
            throw new IllegalArgumentException("Wildcard origin cannot be used with credentials.");
        }
    }

    private static void validateCorsOrigins(String origins, boolean allowCredentials) {
        if (origins == null || origins.isBlank()) {
            return;
        }
        for (String origin : origins.split(",")) {
            validateCorsOrigin(origin.trim(), allowCredentials);
        }
    }

    private static void validateRiskRule(SecuritySettingsCenterResponse.RiskRuleSettings rule) {
        if (rule == null) {
            throw new IllegalArgumentException("risk rule is required.");
        }
        if (rule.eventType() == null || rule.riskLevel() == null) {
            throw new IllegalArgumentException("risk rule eventType and riskLevel are required.");
        }
        requireScore("riskRules.score", rule.score());
        if (rule.nightStartHour() != null && (rule.nightStartHour() < 0 || rule.nightStartHour() > 23)) {
            throw new IllegalArgumentException("nightStartHour must be between 0 and 23.");
        }
        if (rule.nightEndHour() != null && (rule.nightEndHour() < 0 || rule.nightEndHour() > 23)) {
            throw new IllegalArgumentException("nightEndHour must be between 0 and 23.");
        }
    }

    private static void requireScore(String field, int value) {
        if (value < 0 || value > 100) {
            throw new IllegalArgumentException(field + " must be between 0 and 100.");
        }
    }

    private static void requirePositive(String field, int value) {
        if (value <= 0) {
            throw new IllegalArgumentException(field + " must be positive.");
        }
    }

    private static void requirePresent(String field, Object value) {
        if (value == null) {
            throw new IllegalArgumentException(field + " is required.");
        }
    }
}
