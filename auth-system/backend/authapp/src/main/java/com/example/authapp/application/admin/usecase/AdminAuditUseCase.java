package com.example.authapp.application.admin.usecase;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import com.example.authapp.domain.admin.dto.AdminActionLogResponse;
import com.example.authapp.domain.admin.dto.AdminAuditLogResponse;
import com.example.authapp.domain.admin.dto.AdminLoginHistoryResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminAuditUseCase {

    private final AdminConsoleUseCase adminConsoleUseCase;

    public Page<AdminAuditLogResponse> auditLogs(int page, int size, String username, String type, String from, String to, String sort, String direction) {
        return adminConsoleUseCase.auditLogs(page, size, username, type, from, to, sort, direction);
    }

    public Page<AdminActionLogResponse> adminActionLogs(int page, int size, String actor, String target, String action, String result, String reason, String riskLevel, String ipAddress, String userAgent, String from, String to, String sort, String direction) {
        return adminConsoleUseCase.adminActionLogs(page, size, actor, target, action, result, reason, riskLevel, ipAddress, userAgent, from, to, sort, direction);
    }

    public Page<AdminLoginHistoryResponse> loginHistory(int page, int size, String username, String status, String from, String to, String sort, String direction) {
        return adminConsoleUseCase.loginHistory(page, size, username, status, from, to, sort, direction);
    }
}
