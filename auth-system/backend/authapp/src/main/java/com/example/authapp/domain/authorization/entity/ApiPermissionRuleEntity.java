package com.example.authapp.domain.authorization.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "api_permission_rules",
        indexes = {
                @Index(name = "idx_api_permission_rules_method", columnList = "http_method"),
                @Index(name = "idx_api_permission_rules_enabled", columnList = "enabled"),
                @Index(name = "idx_api_permission_rules_sort", columnList = "sort_order")
        },
        uniqueConstraints = @UniqueConstraint(
                name = "uk_api_permission_rule",
                columnNames = {"http_method", "path_pattern", "permission_code"}
        )
)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiPermissionRuleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "http_method", nullable = false, length = 20)
    private String httpMethod;

    @Column(name = "path_pattern", nullable = false, length = 300)
    private String pathPattern;

    @Column(name = "permission_code", nullable = false, length = 100)
    private String permissionCode;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void update(
            String httpMethod,
            String pathPattern,
            String permissionCode,
            String description,
            boolean enabled,
            int sortOrder
    ) {
        this.httpMethod = httpMethod;
        this.pathPattern = pathPattern;
        this.permissionCode = permissionCode;
        this.description = description;
        this.enabled = enabled;
        this.sortOrder = sortOrder;
    }
}
