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
        name = "mfa_exceptions",
        indexes = {
                @Index(name = "idx_mfa_exception_username", columnList = "username"),
                @Index(name = "idx_mfa_exception_expires", columnList = "expires_at")
        }
)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MfaExceptionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String username;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "revoked_by", length = 100)
    private String revokedBy;

    public boolean isActive() {
        return revokedAt == null && expiresAt != null && expiresAt.isAfter(LocalDateTime.now());
    }

    public void revoke(String actor) {
        this.revokedAt = LocalDateTime.now();
        this.revokedBy = actor;
    }
}
