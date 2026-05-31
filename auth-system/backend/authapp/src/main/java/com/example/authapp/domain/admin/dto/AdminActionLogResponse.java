package com.example.authapp.domain.admin.dto;

import java.time.LocalDateTime;

import com.example.authapp.domain.audit.entity.AdminActionLogEntity;

public record AdminActionLogResponse(
        Long id,
        String actorUsername,
        String targetUsername,
        String actionType,
        String reason,
        String beforeValue,
        String afterValue,
        String ipAddress,
        String device,
        LocalDateTime createdAt
) {
    public static AdminActionLogResponse from(AdminActionLogEntity log) {
        return new AdminActionLogResponse(
                log.getId(),
                log.getActorUsername(),
                log.getTargetUsername(),
                log.getActionType().name(),
                log.getReason(),
                log.getBeforeValue(),
                log.getAfterValue(),
                log.getIpAddress(),
                log.getDevice(),
                log.getCreatedAt()
        );
    }
}
