package com.example.authapp.domain.authorization.dto;

public record AdminRoleAssignmentRequest(
        Long roleId,
        String roleName,
        String reason,
        String sensitiveReason
) {
}
