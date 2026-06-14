package com.example.authapp.domain.admin.dto;

// 관리자 대시보드 상단 KPI 카드에 필요한 집계 DTO
public record AdminDashboardSummaryResponse(
        long totalUsers,
        long activeUsers,
        long lockedUsers,
        long adminUsers,
        long totalSessions,
        long revokedSessions,
        long openIncidents,
        long highRiskUsers,
        long unresolvedRiskEvents,
        long criticalRiskEventsToday
) {}
