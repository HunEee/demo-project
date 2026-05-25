package com.example.authapp.domain.admin.dto;

import java.util.List;

// 사용자 상세 화면에서 기본 정보와 최근 보안 정보를 한 번에 보여주기 위한 조합 DTO
public record AdminUserDetailResponse(
        AdminUserResponse user,
        List<AdminLoginHistoryResponse> recentLogins,
        List<AdminAuditLogResponse> recentEvents,
        List<AdminSessionResponse> sessions,
        AdminRiskResponse risk
) {}
