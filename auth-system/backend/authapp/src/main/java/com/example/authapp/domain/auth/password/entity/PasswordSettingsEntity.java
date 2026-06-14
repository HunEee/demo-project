package com.example.authapp.domain.auth.password.entity;

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
@Table(name = "password_settings")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordSettingsEntity {

    public static final long SETTINGS_ID = 1L;

    @Id
    @Column(name = "settings_id")
    private Long id;

    @Column(name = "min_length", nullable = false)
    private int minLength;

    @Column(name = "require_uppercase", nullable = false)
    private boolean requireUppercase;

    @Column(name = "require_lowercase", nullable = false)
    private boolean requireLowercase;

    @Column(name = "require_digit", nullable = false)
    private boolean requireDigit;

    @Column(name = "require_special", nullable = false)
    private boolean requireSpecial;

    @Column(name = "history_count", nullable = false)
    private int historyCount;

    @Column(name = "expiration_days", nullable = false)
    private int expirationDays;

    @Column(name = "temporary_password_expiration_minutes", nullable = false)
    private int temporaryPasswordExpirationMinutes;

    @Column(name = "block_username_email_inclusion", nullable = false)
    private boolean blockUsernameEmailInclusion;

    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public static PasswordSettingsEntity defaults() {
        return PasswordSettingsEntity.builder()
                .id(SETTINGS_ID)
                .minLength(10)
                .requireUppercase(true)
                .requireLowercase(true)
                .requireDigit(true)
                .requireSpecial(true)
                .historyCount(5)
                .expirationDays(90)
                .temporaryPasswordExpirationMinutes(30)
                .blockUsernameEmailInclusion(true)
                .build();
    }
}
