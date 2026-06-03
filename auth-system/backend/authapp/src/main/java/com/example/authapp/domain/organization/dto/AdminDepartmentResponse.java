package com.example.authapp.domain.organization.dto;

import java.time.LocalDateTime;

import com.example.authapp.domain.organization.entity.DepartmentEntity;

public record AdminDepartmentResponse(
        Long id,
        String name,
        String code,
        Long parentId,
        String parentName,
        String managerUsername,
        boolean enabled,
        int displayOrder,
        long userCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AdminDepartmentResponse from(DepartmentEntity department, long userCount) {
        DepartmentEntity parent = department.getParent();
        return new AdminDepartmentResponse(
                department.getId(),
                department.getName(),
                department.getCode(),
                parent != null ? parent.getId() : null,
                parent != null ? parent.getName() : null,
                department.getManagerUsername(),
                department.isEnabled(),
                department.getDisplayOrder(),
                userCount,
                department.getCreatedAt(),
                department.getUpdatedAt()
        );
    }
}
