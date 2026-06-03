package com.example.authapp.domain.authorization.dto;

public record AdminRolePermissionRequest(
        Long permissionId,
        String permissionCode,
        String reason
) {
}
