package com.example.authapp.domain.organization.entity;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.example.authapp.domain.authorization.entity.RoleEntity;

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
        name = "user_groups",
        indexes = {
                @Index(name = "idx_group_name", columnList = "name", unique = true),
                @Index(name = "idx_group_owner", columnList = "owner_username")
        }
)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false, length = 30)
    private String type;

    @Column(name = "owner_username", length = 100)
    private String ownerUsername;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private boolean enabled;

    @Builder.Default
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "group_roles",
            joinColumns = @JoinColumn(name = "group_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<RoleEntity> roles = new HashSet<>();

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void update(String name, String type, String ownerUsername, String description, boolean enabled) {
        this.name = name;
        this.type = type;
        this.ownerUsername = ownerUsername;
        this.description = description;
        this.enabled = enabled;
    }

    public void disable() {
        this.enabled = false;
    }

    public void addRole(RoleEntity role) {
        if (role == null) return;
        this.roles.add(role);
    }

    public void removeRole(RoleEntity role) {
        if (role == null) return;
        this.roles.remove(role);
    }
}
