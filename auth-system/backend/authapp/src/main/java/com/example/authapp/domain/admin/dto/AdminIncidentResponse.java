package com.example.authapp.domain.admin.dto;

import java.time.LocalDateTime;

import com.example.authapp.domain.audit.entity.SecurityIncidentEntity;

// 보안 사고 목록/상세 화면에서 사용하는 응답 DTO
public record AdminIncidentResponse(
        Long id,
        String username,
        String type,
        String severity,
        String description,
        String ipAddress,
        String device,
        boolean resolved,
        String resolvedBy,
        LocalDateTime resolvedAt,
        LocalDateTime createdAt
) {
    public static AdminIncidentResponse from(SecurityIncidentEntity incident) {
        return new AdminIncidentResponse(
                incident.getId(),
                incident.getUsername(),
                incident.getType().name(),
                incident.getSeverity().name(),
                incident.getDescription(),
                incident.getIpAddress(),
                incident.getDevice(),
                incident.isResolved(),
                incident.getResolvedBy(),
                incident.getResolvedAt(),
                incident.getCreatedAt()
        );
    }
}
