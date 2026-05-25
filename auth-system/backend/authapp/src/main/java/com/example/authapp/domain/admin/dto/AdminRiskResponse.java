package com.example.authapp.domain.admin.dto;

import java.time.LocalDateTime;

import com.example.authapp.domain.risk.entity.RiskEntity;

// 위험 사용자 관리 화면에서 사용하는 응답 DTO
public record AdminRiskResponse(
        Long id,
        String username,
        int riskScore,
        String riskLevel,
        String lastReason,
        LocalDateTime updatedAt
) {
    public static AdminRiskResponse from(RiskEntity risk) {
        return new AdminRiskResponse(
                risk.getId(),
                risk.getUsername(),
                risk.getRiskScore(),
                risk.getRiskLevel() == null ? "LOW" : risk.getRiskLevel().name(),
                risk.getLastReason(),
                risk.getUpdatedAt()
        );
    }
}
