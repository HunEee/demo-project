package com.example.authapp.domain.admin.dto;

import java.time.LocalDateTime;

import com.example.authapp.domain.audit.entity.LoginHistoryEntity;

// 관리자 로그인 이력 테이블에서 사용하는 응답 DTO
public record AdminLoginHistoryResponse(
        Long id,
        String username,
        LocalDateTime loginAt,
        LocalDateTime logoutAt,
        boolean success,
        String status,
        String failReason,
        String ipAddress,
        String device,
        String location
) {
    public static AdminLoginHistoryResponse from(LoginHistoryEntity history) {
        return new AdminLoginHistoryResponse(
                history.getId(),
                history.getUsername(),
                history.getLoginAt(),
                history.getLogoutAt(),
                history.isSuccess(),
                history.getStatus().name(),
                history.getFailReason(),
                history.getIpAddress(),
                history.getDevice(),
                history.getLocation()
        );
    }
}
