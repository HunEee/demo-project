package com.example.authapp.domain.auth.lockout.entity;

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
@Table(name = "account_lockout_settings")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountLockoutSettingsEntity {

    public static final long SETTINGS_ID = 1L;

    @Id
    @Column(name = "settings_id")
    private Long id;

    @Column(name = "failed_login_threshold", nullable = false)
    private int failedLoginThreshold;

    @Column(name = "failure_window_minutes", nullable = false)
    private int failureWindowMinutes;

    @Column(name = "lock_duration_minutes", nullable = false)
    private int lockDurationMinutes;

    @Column(name = "admin_failed_login_threshold", nullable = false)
    private int adminFailedLoginThreshold;

    @Column(name = "auto_unlock_enabled", nullable = false)
    private boolean autoUnlockEnabled;

    @Column(name = "manual_unlock_audit_required", nullable = false)
    private boolean manualUnlockAuditRequired;

    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public static AccountLockoutSettingsEntity defaults() {
        return AccountLockoutSettingsEntity.builder()
                .id(SETTINGS_ID)
                .failedLoginThreshold(5)
                .failureWindowMinutes(5)
                .lockDurationMinutes(30)
                .adminFailedLoginThreshold(3)
                .autoUnlockEnabled(true)
                .manualUnlockAuditRequired(true)
                .build();
    }
}
