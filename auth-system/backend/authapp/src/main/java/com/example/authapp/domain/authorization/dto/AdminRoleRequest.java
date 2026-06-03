package com.example.authapp.domain.authorization.dto;

public record AdminRoleRequest(
        String name,
        String displayName,
        String description,
        Boolean enabled,
        Boolean systemRole,
        String reason
) {
}
