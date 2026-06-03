package com.example.authapp.domain.authorization.entity;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.example.authapp.domain.user.entity.UserEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "roles",
        indexes = {
                @Index(name = "idx_roles_enabled", columnList = "enabled")
        }
)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    private Long id;

    @Column(unique = true, nullable = false, length = 100)
    private String name;

    @Column(name = "display_name", length = 100)
    private String displayName;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "system_role", nullable = false)
    private boolean systemRole;

    @Builder.Default
    @ManyToMany(mappedBy = "roles")
    private Set<UserEntity> users = new HashSet<>();

    @Builder.Default
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "role_permissions",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<PermissionEntity> permissions = new HashSet<>();

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void update(String name, String displayName, String description, boolean enabled, boolean systemRole) {
        this.name = name;
        this.displayName = displayName;
        this.description = description;
        this.enabled = enabled;
        this.systemRole = systemRole;
    }

    public void disable() {
        this.enabled = false;
    }

    public void addPermission(PermissionEntity permission) {
        if (permission == null) return;
        if (this.permissions.add(permission)) {
            permission.getRoles().add(this);
        }
    }

    public void removePermission(PermissionEntity permission) {
        if (permission == null) return;
        if (this.permissions.remove(permission)) {
            permission.getRoles().remove(this);
        }
    }

    public boolean hasSensitivePermission() {
        return permissions.stream().anyMatch(PermissionEntity::isSensitive);
    }
}
