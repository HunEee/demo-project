package com.example.authapp.domain.mfa.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "mfa_challenges",
        indexes = {
                @Index(name = "idx_mfa_challenge_id", columnList = "challenge_id"),
                @Index(name = "idx_mfa_challenge_username", columnList = "username")
        }
)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MfaChallengeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "challenge_id", nullable = false, unique = true, length = 80)
    private String challengeId;

    @Column(nullable = false, length = 100)
    private String username;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean verified;

    @Column(nullable = false)
    private boolean used;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

    @Column(name = "last_failed_at")
    private LocalDateTime lastFailedAt;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "request_fingerprint", length = 128)
    private String requestFingerprint;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }

    public void markVerifiedAndUsed() {
        this.verified = true;
        this.used = true;
    }

    public int recordFailedAttempt() {
        this.failedAttempts++;
        this.lastFailedAt = LocalDateTime.now();
        return failedAttempts;
    }

    public void markUsed() {
        this.used = true;
    }
}
