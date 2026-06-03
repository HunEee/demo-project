package com.example.authapp.domain.authorization.entity;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

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
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "permissions",
        indexes = {
                @Index(name = "idx_permissions_category", columnList = "category"),
                @Index(name = "idx_permissions_enabled", columnList = "enabled"),
                @Index(name = "idx_permissions_sensitive", columnList = "sensitive")
        }
)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "permission_id")
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(length = 500)
    private String description;

    @Column(name = "`sensitive`", nullable = false)
    private boolean sensitive;

    @Column(nullable = false)
    private boolean enabled;

    @Builder.Default
    @ManyToMany(mappedBy = "permissions")
    private Set<RoleEntity> roles = new HashSet<>();

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void update(String code, String name, String category, String description, boolean sensitive, boolean enabled) {
        this.code = code;
        this.name = name;
        this.category = category;
        this.description = description;
        this.sensitive = sensitive;
        this.enabled = enabled;
    }
}
