package com.example.authapp.domain.jwt.entity;

import java.time.LocalDateTime;

import com.example.authapp.domain.audit.entity.LoginHistoryEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "refresh_token",
        indexes = {
                @Index(name = "idx_refresh_token_hash", columnList = "refresh_token_hash", unique = true),
                @Index(name = "idx_refresh_username", columnList = "username"),
                @Index(name = "idx_refresh_expires_at", columnList = "expires_at")
        }
)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "refresh_token_hash", length = 64)
    private String refreshTokenHash;

    @Column(nullable = false, unique = true)
    private String jti;

    @Column(name = "family_id", nullable = false, length = 64)
    private String familyId;

    @Column(name = "token_sequence", nullable = false)
    private long tokenSequence;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "revoked", nullable = false)
    private boolean revoked;

    @Column(name = "revoked_reason", length = 100)
    private String revokedReason;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "rotated_at")
    private LocalDateTime rotatedAt;

    @Column(name = "rotation_grace_until")
    private LocalDateTime rotationGraceUntil;

    @Column(name = "reuse_detected_at")
    private LocalDateTime reuseDetectedAt;

    @Column(name = "revoked_by", length = 100)
    private String revokedBy;

    @Column(name = "replaced_by_token", length = 512)
    private String replacedByToken;

    @Column(name = "device")
    private String device;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "login_history_id")
    private LoginHistoryEntity loginHistory;

    public void revoke() {
        revoke("REVOKED");
    }

    public void revoke(String reason) {
        revokeBy(reason, "SYSTEM");
    }

    public void revokeBy(String reason, String actorUsername) {
        this.revoked = true;
        this.revokedReason = reason;
        this.revokedBy = actorUsername;
        this.revokedAt = LocalDateTime.now();
    }

    public void setReplacedByToken(String replacedByToken) {
        this.replacedByToken = replacedByToken;
    }

    public void rotateTo(String replacementJti, String reason, LocalDateTime now, LocalDateTime graceUntil) {
        this.revoked = true;
        this.revokedReason = reason;
        this.revokedBy = "SYSTEM";
        this.revokedAt = now;
        this.rotatedAt = now;
        this.rotationGraceUntil = graceUntil;
        this.replacedByToken = replacementJti;
    }

    public void rotateTo(String replacementJti, LocalDateTime now, LocalDateTime graceUntil) {
        rotateTo(replacementJti, "ROTATED_UNKNOWN", now, graceUntil);
    }

    public void markReuseDetected(LocalDateTime now) {
        this.reuseDetectedAt = now;
    }

    public boolean isRecentlyRotated(LocalDateTime now) {
        return this.revoked
                && this.revokedReason != null
                && this.revokedReason.startsWith("ROTATED")
                && this.replacedByToken != null
                && this.rotationGraceUntil != null
                && !this.rotationGraceUntil.isBefore(now);
    }

    public void updateLastUsedAt() {
        markUsed();
    }

    public void markUsed() {
        this.lastUsedAt = LocalDateTime.now();
    }
}
