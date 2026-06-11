package com.example.authapp.domain.risk.dto;

import java.time.LocalDateTime;

import com.example.authapp.domain.risk.entity.RiskActionLogEntity;

public record RiskActionLogResponse(
        Long id,
        String username,
        Long riskId,
        Long riskEventId,
        String riskLevel,
        String action,
        String mode,
        String status,
        String reason,
        String actorUsername,
        String ipAddress,
        String device,
        LocalDateTime createdAt
) {
    public static RiskActionLogResponse from(RiskActionLogEntity log) {
        return new RiskActionLogResponse(
                log.getId(),
                log.getUsername(),
                log.getRiskId(),
                log.getRiskEventId(),
                log.getRiskLevel(),
                log.getAction(),
                log.getMode(),
                log.getStatus(),
                log.getReason(),
                log.getActorUsername(),
                log.getIpAddress(),
                log.getDevice(),
                log.getCreatedAt()
        );
    }
}
