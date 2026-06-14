package com.example.authapp.domain.auth.verification.entity;

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
@Table(name = "verification_token_settings")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationTokenSettingsEntity {

    public static final long SETTINGS_ID = 1L;

    @Id
    @Column(name = "settings_id")
    private Long id;

    @Column(name = "signup_token_expiration_minutes", nullable = false)
    private int signupTokenExpirationMinutes;

    @Column(name = "password_reset_token_expiration_minutes", nullable = false)
    private int passwordResetTokenExpirationMinutes;

    @Column(name = "max_verification_attempts", nullable = false)
    private int maxVerificationAttempts;

    @Column(name = "resend_cooldown_seconds", nullable = false)
    private int resendCooldownSeconds;

    @Column(name = "daily_send_limit_per_email", nullable = false)
    private int dailySendLimitPerEmail;

    @Column(name = "invalidate_previous_token_on_resend", nullable = false)
    private boolean invalidatePreviousTokenOnResend;

    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public static VerificationTokenSettingsEntity defaults() {
        return VerificationTokenSettingsEntity.builder()
                .id(SETTINGS_ID)
                .signupTokenExpirationMinutes(30)
                .passwordResetTokenExpirationMinutes(15)
                .maxVerificationAttempts(5)
                .resendCooldownSeconds(60)
                .dailySendLimitPerEmail(10)
                .invalidatePreviousTokenOnResend(true)
                .build();
    }
}
