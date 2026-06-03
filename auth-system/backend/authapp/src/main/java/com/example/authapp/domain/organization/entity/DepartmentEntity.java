package com.example.authapp.domain.organization.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

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

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "departments",
        indexes = {
                @Index(name = "idx_department_code", columnList = "code", unique = true),
                @Index(name = "idx_department_parent", columnList = "parent_id"),
                @Index(name = "idx_department_manager", columnList = "manager_username")
        }
)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private DepartmentEntity parent;

    @Column(name = "manager_username", length = 100)
    private String managerUsername;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void update(String name, String code, DepartmentEntity parent, String managerUsername, boolean enabled, int displayOrder) {
        this.name = name;
        this.code = code;
        this.parent = parent;
        this.managerUsername = managerUsername;
        this.enabled = enabled;
        this.displayOrder = displayOrder;
    }

    public void disable() {
        this.enabled = false;
    }
}
