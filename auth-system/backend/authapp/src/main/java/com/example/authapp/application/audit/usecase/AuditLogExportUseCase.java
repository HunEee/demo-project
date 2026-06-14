package com.example.authapp.application.audit.usecase;

import org.springframework.stereotype.Service;

import com.example.authapp.domain.audit.service.AuditLogExportService;
import com.example.authapp.domain.audit.service.AuditLogExportService.AuditExportFile;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuditLogExportUseCase {

    private final AuditLogExportService auditLogExportService;

    public AuditExportFile exportAdminActionLogs(
            String actor,
            String target,
            String action,
            String result,
            String reason,
            String riskLevel,
            String ipAddress,
            String userAgent,
            String from,
            String to,
            String sort,
            String direction
    ) {
        return auditLogExportService.exportAdminActionLogs(actor, target, action, result, reason, riskLevel, ipAddress, userAgent, from, to, sort, direction);
    }
}
