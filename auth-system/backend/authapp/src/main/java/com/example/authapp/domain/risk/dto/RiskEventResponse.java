package com.example.authapp.domain.risk.dto;

import java.time.LocalDateTime;

import com.example.authapp.domain.risk.entity.RiskEventEntity;

public record RiskEventResponse(
        Long id,
        String username,
        String eventType,
        String riskLevel,
        int score,
        String description,
        String reason,
        String ipAddress,
        String userAgent,
        String device,
        boolean resolved,
        String resolvedBy,
        LocalDateTime resolvedAt,
        LocalDateTime createdAt
) {
    public static RiskEventResponse from(RiskEventEntity event) {
        return new RiskEventResponse(
                event.getId(),
                event.getUsername(),
                event.getEventType() == null ? null : event.getEventType().name(),
                event.getRiskLevel() == null ? null : event.getRiskLevel().name(),
                event.getScore(),
                event.getDescription(),
                event.getReason(),
                event.getIpAddress(),
                event.getUserAgent(),
                event.getDevice(),
                event.isResolved(),
                event.getResolvedBy(),
                event.getResolvedAt(),
                event.getCreatedAt()
        );
    }
}
