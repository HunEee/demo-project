package com.example.authapp.domain.admin.dto;

// 보안 운영 정책 설정 화면의 현재 값을 담는 DTO
public record AdminSettingsResponse(
        int maxLoginFailures,
        int highRiskThreshold,
        int criticalRiskThreshold,
        int sessionExpireDays,
        boolean forceLogoutOnCriticalRisk
) {}
