package com.example.authapp.domain.jwt.entity;

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
@Table(name = "token_settings")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenSettingsEntity {

    public static final long SETTINGS_ID = 1L;

    @Id
    @Column(name = "settings_id")
    private Long id;

    @Column(name = "access_token_lifetime_minutes", nullable = false)
    private int accessTokenLifetimeMinutes;

    @Column(name = "refresh_token_lifetime_days", nullable = false)
    private int refreshTokenLifetimeDays;

    @Column(name = "rotation_enabled", nullable = false)
    private boolean rotationEnabled;

    @Column(name = "rotation_grace_seconds", nullable = false)
    private int rotationGraceSeconds;

    @Column(name = "allow_recent_rotation_recovery", nullable = false)
    private boolean allowRecentRotationRecovery;

    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public static TokenSettingsEntity defaults() {
        return TokenSettingsEntity.builder()
                .id(SETTINGS_ID)
                .accessTokenLifetimeMinutes(60)
                .refreshTokenLifetimeDays(7)
                .rotationEnabled(true)
                .rotationGraceSeconds(30)
                .allowRecentRotationRecovery(true)
                .build();
    }
}
