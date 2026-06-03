package com.example.authapp.domain.authorization.entity;

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
        name = "role_assignment_history",
        indexes = {
                @Index(name = "idx_role_assignment_target", columnList = "target_type,target_id"),
                @Index(name = "idx_role_assignment_role", columnList = "role_id"),
                @Index(name = "idx_role_assignment_actor", columnList = "actor_username"),
                @Index(name = "idx_role_assignment_created", columnList = "created_at")
        }
)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleAssignmentHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "target_type", nullable = false, length = 20)
    private String targetType;

    @Column(name = "target_id", nullable = false, length = 100)
    private String targetId;

    @Column(name = "target_name", length = 200)
    private String targetName;

    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @Column(name = "role_name", nullable = false, length = 100)
    private String roleName;

    @Column(nullable = false, length = 20)
    private String action;

    @Column(name = "actor_username", nullable = false, length = 100)
    private String actorUsername;

    @Column(length = 500)
    private String reason;

    @Column(name = "`sensitive`", nullable = false)
    private boolean sensitive;

    @Column(name = "sensitive_reason", length = 500)
    private String sensitiveReason;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
