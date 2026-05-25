package com.example.authapp.domain.admin.dto;

import java.time.LocalDateTime;

import com.example.authapp.domain.audit.entity.AuthEventLogEntity;

// 관리자 감사 로그 목록에서 사용하는 경량 응답 DTO
public record AdminAuditLogResponse(
        Long id,
        String username,
        String type,
        String description,
        String ipAddress,
        String device,
        LocalDateTime createdAt
) {
    public static AdminAuditLogResponse from(AuthEventLogEntity event) {
        return new AdminAuditLogResponse(
                event.getId(),
                event.getUsername(),
                event.getType().name(),
                event.getDescription(),
                event.getIpAddress(),
                event.getDevice(),
                event.getCreatedAt()
        );
    }
}
