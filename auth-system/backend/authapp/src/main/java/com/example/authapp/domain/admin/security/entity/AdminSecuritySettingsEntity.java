package com.example.authapp.domain.admin.security.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "admin_security_settings")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminSecuritySettingsEntity {

    public static final long SETTINGS_ID = 1L;

    @Id
    @Column(name = "settings_id")
    private Long id;

    @Column(name = "admin_ip_allowlist_enabled", nullable = false)
    private boolean adminIpAllowlistEnabled;

    @Column(name = "deny_unknown_proxy_headers", nullable = false)
    private boolean denyUnknownProxyHeaders;

    @Column(name = "trusted_proxy_header_mode", nullable = false)
    private String trustedProxyHeaderMode;

    @Column(name = "require_mfa_for_admin_access", nullable = false)
    private boolean requireMfaForAdminAccess;

    @Column(name = "admin_session_max_age_minutes", nullable = false)
    private int adminSessionMaxAgeMinutes;

    @Column(name = "allowed_admin_ips", length = 2000)
    private String allowedAdminIps;

    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public static AdminSecuritySettingsEntity defaults() {
        return AdminSecuritySettingsEntity.builder()
                .id(SETTINGS_ID)
                .adminIpAllowlistEnabled(false)
                .denyUnknownProxyHeaders(true)
                .trustedProxyHeaderMode("DISABLED")
                .requireMfaForAdminAccess(false)
                .adminSessionMaxAgeMinutes(480)
                .allowedAdminIps("")
                .build();
    }
}
