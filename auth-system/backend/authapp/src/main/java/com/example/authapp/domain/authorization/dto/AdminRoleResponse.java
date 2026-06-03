package com.example.authapp.domain.authorization.dto;

import java.time.LocalDateTime;

import com.example.authapp.domain.authorization.entity.RoleEntity;

public record AdminRoleResponse(
        Long id,
        String name,
        String displayName,
        String description,
        boolean enabled,
        boolean systemRole,
        boolean sensitive,
        int permissionCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AdminRoleResponse from(RoleEntity role) {
        return new AdminRoleResponse(
                role.getId(),
                role.getName(),
                role.getDisplayName(),
                role.getDescription(),
                role.isEnabled(),
                role.isSystemRole(),
                role.hasSensitivePermission(),
                role.getPermissions().size(),
                role.getCreatedAt(),
                role.getUpdatedAt()
        );
    }
}
