package com.example.authapp.domain.authorization.dto;

public record AdminApiPermissionRuleRequest(
        String httpMethod,
        String pathPattern,
        String permissionCode,
        String description,
        Boolean enabled,
        Integer sortOrder,
        String reason
) {
}
