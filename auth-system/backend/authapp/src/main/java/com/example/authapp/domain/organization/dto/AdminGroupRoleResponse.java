package com.example.authapp.domain.organization.dto;

import java.time.LocalDateTime;

import com.example.authapp.domain.authorization.entity.RoleEntity;

public record AdminGroupRoleResponse(
        Long roleId,
        String roleName,
        LocalDateTime createdAt
) {
    public static AdminGroupRoleResponse from(RoleEntity role) {
        return new AdminGroupRoleResponse(
                role.getId(),
                role.getName(),
                null
        );
    }
}
