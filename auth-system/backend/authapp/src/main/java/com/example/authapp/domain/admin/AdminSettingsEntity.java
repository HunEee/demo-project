package com.example.authapp.domain.admin;

import com.example.authapp.domain.admin.dto.AdminSettingsResponse;
import com.example.authapp.domain.mfa.entity.MfaPolicy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "admin_settings")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminSettingsEntity {

    @Id
    private Long id;

    @Column(name = "max_login_failures", nullable = false)
    private int maxLoginFailures;

    @Column(name = "high_risk_threshold", nullable = false)
    private int highRiskThreshold;

    @Column(name = "critical_risk_threshold", nullable = false)
    private int criticalRiskThreshold;

    @Column(name = "session_expire_days", nullable = false)
    private int sessionExpireDays;

    @Column(name = "force_logout_on_critical_risk", nullable = false)
    private boolean forceLogoutOnCriticalRisk;

    @Enumerated(EnumType.STRING)
    @Column(name = "mfa_policy", nullable = false, length = 40)
    private MfaPolicy mfaPolicy;

    public static AdminSettingsEntity defaults() {
        return AdminSettingsEntity.builder()
                .id(1L)
                .maxLoginFailures(5)
                .highRiskThreshold(60)
                .criticalRiskThreshold(80)
                .sessionExpireDays(14)
                .forceLogoutOnCriticalRisk(true)
                .mfaPolicy(MfaPolicy.OPTIONAL)
                .build();
    }

    public AdminSettingsResponse toResponse() {
        return new AdminSettingsResponse(
                maxLoginFailures,
                highRiskThreshold,
                criticalRiskThreshold,
                sessionExpireDays,
                forceLogoutOnCriticalRisk,
                mfaPolicy == null ? MfaPolicy.OPTIONAL : mfaPolicy.normalized()
        );
    }

    public void update(AdminSettingsResponse request) {
        this.maxLoginFailures = request.maxLoginFailures();
        this.highRiskThreshold = request.highRiskThreshold();
        this.criticalRiskThreshold = request.criticalRiskThreshold();
        this.sessionExpireDays = request.sessionExpireDays();
        this.forceLogoutOnCriticalRisk = request.forceLogoutOnCriticalRisk();
        this.mfaPolicy = request.mfaPolicy() == null ? MfaPolicy.OPTIONAL : request.mfaPolicy().normalized();
    }

    public void updateMfaPolicy(MfaPolicy mfaPolicy) {
        this.mfaPolicy = mfaPolicy == null ? MfaPolicy.OPTIONAL : mfaPolicy.normalized();
    }
}
