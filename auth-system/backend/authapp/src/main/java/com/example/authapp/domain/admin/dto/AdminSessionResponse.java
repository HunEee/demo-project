package com.example.authapp.domain.admin.dto;

import java.time.LocalDateTime;

import com.example.authapp.domain.jwt.entity.RefreshTokenEntity;

// 관리자 세션/토큰 관리 화면에서 사용하는 응답 DTO
public record AdminSessionResponse(
        Long id,
        String username,
        String jti,
        String device,
        String ipAddress,
        boolean revoked,
        String revokedReason,
        LocalDateTime revokedAt,
        String revokedBy,
        LocalDateTime createdAt,
        LocalDateTime expiresAt,
        LocalDateTime lastUsedAt
) {
    public static AdminSessionResponse from(RefreshTokenEntity token) {
        return new AdminSessionResponse(
                token.getId(),
                token.getUsername(),
                token.getJti(),
                token.getDevice(),
                token.getIpAddress(),
                token.isRevoked(),
                token.getRevokedReason(),
                token.getRevokedAt(),
                token.getRevokedBy(),
                token.getCreatedAt(),
                token.getExpiresAt(),
                token.getLastUsedAt()
        );
    }
}
