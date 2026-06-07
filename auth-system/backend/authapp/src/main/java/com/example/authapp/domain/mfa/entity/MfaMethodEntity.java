package com.example.authapp.domain.mfa.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "mfa_methods",
        indexes = {
                @Index(name = "idx_mfa_username", columnList = "username"),
                @Index(name = "idx_mfa_username_enabled", columnList = "username, enabled")
        }
)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MfaMethodEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String username;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MfaMethodType type;

    @Column(nullable = false)
    private boolean enabled;

    @Lob
    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "secret")
    private String secret;

    @Column(name = "registered_at")
    private LocalDateTime registeredAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void confirmRegistration() {
        this.enabled = true;
        this.registeredAt = LocalDateTime.now();
    }

    public void markUsed() {
        this.lastUsedAt = LocalDateTime.now();
    }
}
