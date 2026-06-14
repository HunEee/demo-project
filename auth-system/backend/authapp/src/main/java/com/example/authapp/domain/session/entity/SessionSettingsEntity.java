package com.example.authapp.domain.session.entity;

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
@Table(name = "session_settings")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionSettingsEntity {

    public static final long SETTINGS_ID = 1L;

    @Id
    @Column(name = "settings_id")
    private Long id;

    @Column(name = "max_active_sessions_user", nullable = false)
    private int maxActiveSessionsUser;

    @Column(name = "max_active_sessions_admin", nullable = false)
    private int maxActiveSessionsAdmin;

    @Column(name = "idle_timeout_minutes", nullable = false)
    private int idleTimeoutMinutes;

    @Column(name = "revoke_on_password_change", nullable = false)
    private boolean revokeOnPasswordChange;

    @Column(name = "revoke_on_mfa_reset", nullable = false)
    private boolean revokeOnMfaReset;

    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public static SessionSettingsEntity defaults() {
        return SessionSettingsEntity.builder()
                .id(SETTINGS_ID)
                .maxActiveSessionsUser(5)
                .maxActiveSessionsAdmin(3)
                .idleTimeoutMinutes(120)
                .revokeOnPasswordChange(true)
                .revokeOnMfaReset(true)
                .build();
    }
}
