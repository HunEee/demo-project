package com.example.authapp.domain.authorization.dto;

import java.time.LocalDateTime;

import com.example.authapp.domain.authorization.entity.PermissionEntity;

public record AdminPermissionResponse(
        Long id,
        String code,
        String name,
        String category,
        String description,
        boolean sensitive,
        boolean enabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AdminPermissionResponse from(PermissionEntity permission) {
        return new AdminPermissionResponse(
                permission.getId(),
                permission.getCode(),
                permission.getName(),
                permission.getCategory(),
                permission.getDescription(),
                permission.isSensitive(),
                permission.isEnabled(),
                permission.getCreatedAt(),
                permission.getUpdatedAt()
        );
    }
}
