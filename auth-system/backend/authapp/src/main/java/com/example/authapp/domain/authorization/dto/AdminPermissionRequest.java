package com.example.authapp.domain.authorization.dto;

public record AdminPermissionRequest(
        String code,
        String name,
        String category,
        String description,
        Boolean sensitive,
        Boolean enabled,
        String reason
) {
}
