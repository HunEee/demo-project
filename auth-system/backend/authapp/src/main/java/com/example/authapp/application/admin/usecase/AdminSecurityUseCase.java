package com.example.authapp.application.admin.usecase;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import com.example.authapp.domain.admin.dto.AdminAuditLogResponse;
import com.example.authapp.domain.admin.dto.AdminIncidentResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminSecurityUseCase {

    private final AdminConsoleUseCase adminConsoleUseCase;

    public Page<AdminAuditLogResponse> securityEvents(int page, int size, String username, String type, String from, String to, String sort, String direction) {
        return adminConsoleUseCase.securityEvents(page, size, username, type, from, to, sort, direction);
    }

    public Page<AdminIncidentResponse> incidents(int page, int size, String username, String type, String severity, Boolean resolved, String from, String to, String sort, String direction) {
        return adminConsoleUseCase.incidents(page, size, username, type, severity, resolved, from, to, sort, direction);
    }

    public void resolveIncident(Long id, String adminUsername, String reason) {
        adminConsoleUseCase.resolveIncident(id, adminUsername, reason);
    }
}
