package com.example.authapp.domain.audit.service;

import com.example.authapp.domain.audit.entity.AdminActionType;

import lombok.Builder;

@Builder
public record AdminActionLogRequest(
        String actorUsername,
        String targetType,
        String targetId,
        String targetUsername,
        String targetName,
        AdminActionType actionType,
        String reason,
        String beforeValue,
        String afterValue,
        String ipAddress,
        String device,
        String userAgent,
        String result,
        String riskLevel,
        String metadata
) {
}
