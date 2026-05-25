package com.example.authapp.domain.admin;

import org.springframework.stereotype.Component;

import com.example.authapp.domain.admin.dto.AdminSettingsResponse;

// DB 설정 테이블을 도입하기 전까지 사용하는 간단한 런타임 보안 정책 저장소
@Component
public class AdminSettingsStore {

    private int maxLoginFailures = 5;
    private int highRiskThreshold = 60;
    private int criticalRiskThreshold = 80;
    private int sessionExpireDays = 14;
    private boolean forceLogoutOnCriticalRisk = true;

    public AdminSettingsResponse current() {
        return new AdminSettingsResponse(
                maxLoginFailures,
                highRiskThreshold,
                criticalRiskThreshold,
                sessionExpireDays,
                forceLogoutOnCriticalRisk
        );
    }

    public AdminSettingsResponse update(AdminSettingsResponse request) {
        this.maxLoginFailures = request.maxLoginFailures();
        this.highRiskThreshold = request.highRiskThreshold();
        this.criticalRiskThreshold = request.criticalRiskThreshold();
        this.sessionExpireDays = request.sessionExpireDays();
        this.forceLogoutOnCriticalRisk = request.forceLogoutOnCriticalRisk();
        return current();
    }
    
}
