package com.example.authapp.domain.audit.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
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
        name = "admin_action_log",
        indexes = {
                @Index(name = "idx_admin_action_actor", columnList = "actor_username"),
                @Index(name = "idx_admin_action_target_user", columnList = "target_username"),
                @Index(name = "idx_admin_action_target", columnList = "target_type, target_id"),
                @Index(name = "idx_admin_action_type", columnList = "action_type"),
                @Index(name = "idx_admin_action_created", columnList = "created_at")
        }
)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminActionLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actor_username", nullable = false, length = 100)
    private String actorUsername;

    @Column(name = "target_type", nullable = false, length = 50)
    private String targetType;

    @Column(name = "target_id", length = 100)
    private String targetId;

    @Column(name = "target_username", length = 100)
    private String targetUsername;

    @Column(name = "target_name", length = 200)
    private String targetName;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 50)
    private AdminActionType actionType;

    @Column(length = 500)
    private String reason;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(length = 100)
    private String device;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String result = "SUCCESS";

    @Column(name = "risk_level", length = 30)
    private String riskLevel;

    @Column(name = "after_value", columnDefinition = "TEXT")
    private String afterValue;

    @Column(name = "before_value", columnDefinition = "TEXT")
    private String beforeValue;
    
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
