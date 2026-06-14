package com.example.authapp.domain.mfa.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "mfa_settings")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MfaSettingsEntity {

    public static final long SETTINGS_ID = 1L;

    @Id
    @Column(name = "settings_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "mfa_policy", nullable = false, length = 40)
    private MfaPolicy policy;

    @Column(name = "high_risk_mfa_threshold", nullable = false)
    private int highRiskMfaThreshold;

    @Column(name = "temporary_exception_max_days", nullable = false)
    private int temporaryExceptionMaxDays;

    @Column(name = "challenge_failure_limit", nullable = false)
    private int challengeFailureLimit;

    @Column(name = "challenge_expiration_minutes", nullable = false)
    private int challengeExpirationMinutes;

    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public static MfaSettingsEntity defaults() {
        return MfaSettingsEntity.builder()
                .id(SETTINGS_ID)
                .policy(MfaPolicy.OPTIONAL)
                .highRiskMfaThreshold(60)
                .temporaryExceptionMaxDays(7)
                .challengeFailureLimit(5)
                .challengeExpirationMinutes(5)
                .build();
    }
}
