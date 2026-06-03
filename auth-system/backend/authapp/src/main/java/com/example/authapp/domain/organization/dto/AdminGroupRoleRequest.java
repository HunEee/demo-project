package com.example.authapp.domain.organization.dto;

public record AdminGroupRoleRequest(
        String roleName,
        String reason,
        String sensitiveReason
) {
}
