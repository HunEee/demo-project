package com.example.authapp.domain.authorization.dto;

import java.time.LocalDateTime;

import com.example.authapp.domain.authorization.entity.ApiPermissionRuleEntity;

public record AdminApiPermissionRuleResponse(
        Long id,
        String httpMethod,
        String pathPattern,
        String permissionCode,
        String description,
        boolean enabled,
        int sortOrder,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static AdminApiPermissionRuleResponse from(ApiPermissionRuleEntity rule) {
        return new AdminApiPermissionRuleResponse(
                rule.getId(),
                rule.getHttpMethod(),
                rule.getPathPattern(),
                rule.getPermissionCode(),
                rule.getDescription(),
                rule.isEnabled(),
                rule.getSortOrder(),
                rule.getCreatedAt(),
                rule.getUpdatedAt()
        );
    }
}
