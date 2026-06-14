package com.example.authapp.domain.admin.settings.dto;

import java.util.List;

public record SecuritySettingsUpdateRequest(
        SecuritySettingsCenterResponse.AuthenticationSettings authentication,
        SecuritySettingsCenterResponse.SessionTokenSettings sessionToken,
        SecuritySettingsCenterResponse.RiskDetectionSettings riskDetection,
        List<SecuritySettingsCenterResponse.RiskRuleSettings> riskRules,
        SecuritySettingsCenterResponse.OperationalSecuritySettings operationalSecurity
) {
}
