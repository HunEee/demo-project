package com.example.authapp.domain.admin.dto;

import com.example.authapp.domain.mfa.entity.MfaPolicy;

public record AdminSettingsResponse(
        int maxLoginFailures,
        int highRiskThreshold,
        int criticalRiskThreshold,
        int sessionExpireDays,
        boolean forceLogoutOnCriticalRisk,
        MfaPolicy mfaPolicy
) {}
