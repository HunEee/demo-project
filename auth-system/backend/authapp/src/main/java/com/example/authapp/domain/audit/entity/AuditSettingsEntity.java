package com.example.authapp.domain.audit.entity;

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
@Table(name = "audit_settings")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditSettingsEntity {

    public static final long SETTINGS_ID = 1L;

    @Id
    @Column(name = "settings_id")
    private Long id;

    @Column(name = "audit_log_retention_days", nullable = false)
    private int auditLogRetentionDays;

    @Column(name = "login_history_retention_days", nullable = false)
    private int loginHistoryRetentionDays;

    @Column(name = "risk_event_retention_days", nullable = false)
    private int riskEventRetentionDays;

    @Column(name = "security_incident_retention_days", nullable = false)
    private int securityIncidentRetentionDays;

    @Column(name = "admin_action_log_retention_days", nullable = false)
    private int adminActionLogRetentionDays;

    @Column(name = "export_requires_reason", nullable = false)
    private boolean exportRequiresReason;

    @Column(name = "audit_log_delete_disabled", nullable = false)
    private boolean auditLogDeleteDisabled;

    @Column(name = "archive_before_purge", nullable = false)
    private boolean archiveBeforePurge;

    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public static AuditSettingsEntity defaults() {
        return AuditSettingsEntity.builder()
                .id(SETTINGS_ID)
                .auditLogRetentionDays(365)
                .loginHistoryRetentionDays(180)
                .riskEventRetentionDays(365)
                .securityIncidentRetentionDays(730)
                .adminActionLogRetentionDays(365)
                .exportRequiresReason(true)
                .auditLogDeleteDisabled(true)
                .archiveBeforePurge(true)
                .build();
    }
}
