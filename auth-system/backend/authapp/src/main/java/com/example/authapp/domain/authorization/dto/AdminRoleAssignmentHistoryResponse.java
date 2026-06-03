package com.example.authapp.domain.authorization.dto;

import java.time.LocalDateTime;

import com.example.authapp.domain.authorization.entity.RoleAssignmentHistoryEntity;

public record AdminRoleAssignmentHistoryResponse(
        Long id,
        String targetType,
        String targetId,
        String targetName,
        Long roleId,
        String roleName,
        String action,
        String actorUsername,
        String reason,
        boolean sensitive,
        String sensitiveReason,
        LocalDateTime createdAt
) {
    public static AdminRoleAssignmentHistoryResponse from(RoleAssignmentHistoryEntity history) {
        return new AdminRoleAssignmentHistoryResponse(
                history.getId(),
                history.getTargetType(),
                history.getTargetId(),
                history.getTargetName(),
                history.getRoleId(),
                history.getRoleName(),
                history.getAction(),
                history.getActorUsername(),
                history.getReason(),
                history.isSensitive(),
                history.getSensitiveReason(),
                history.getCreatedAt()
        );
    }
}
