package com.example.authapp.api.admin;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.authapp.domain.admin.AdminConsoleService;
import com.example.authapp.domain.admin.dto.AdminActionLogResponse;
import com.example.authapp.domain.admin.dto.AdminAuditLogResponse;
import com.example.authapp.domain.admin.dto.AdminLoginHistoryResponse;
import com.example.authapp.domain.audit.dto.PageResponseDTO;
import com.example.authapp.domain.audit.service.AuditLogExportService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminAuditController {

    private final AdminConsoleService adminConsoleService;
    private final AuditLogExportService auditLogExportService;

    // 전체 감사 이벤트를 조회
    @GetMapping("/audit-logs")
    public PageResponseDTO<AdminAuditLogResponse> auditLogs(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "username", required = false) String username,
            @RequestParam(name = "type", required = false) String type,
            @RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "to", required = false) String to,
            @RequestParam(name = "sort", required = false) String sort,
            @RequestParam(name = "direction", defaultValue = "DESC") String direction
    ) {
        return new PageResponseDTO<>(adminConsoleService.auditLogs(page, size, username, type, from, to, sort, direction));
    }

    @GetMapping("/action-logs")
    public PageResponseDTO<AdminActionLogResponse> actionLogs(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "actor", required = false) String actor,
            @RequestParam(name = "target", required = false) String target,
            @RequestParam(name = "action", required = false) String action,
            @RequestParam(name = "result", required = false) String result,
            @RequestParam(name = "reason", required = false) String reason,
            @RequestParam(name = "riskLevel", required = false) String riskLevel,
            @RequestParam(name = "ipAddress", required = false) String ipAddress,
            @RequestParam(name = "userAgent", required = false) String userAgent,
            @RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "to", required = false) String to,
            @RequestParam(name = "sort", required = false) String sort,
            @RequestParam(name = "direction", defaultValue = "DESC") String direction
    ) {
        return new PageResponseDTO<>(adminConsoleService.adminActionLogs(page, size, actor, target, action, result, reason, riskLevel, ipAddress, userAgent, from, to, sort, direction));
    }

    @GetMapping("/action-logs/export")
    public ResponseEntity<byte[]> exportActionLogs(
            @RequestParam(name = "actor", required = false) String actor,
            @RequestParam(name = "target", required = false) String target,
            @RequestParam(name = "action", required = false) String action,
            @RequestParam(name = "result", required = false) String result,
            @RequestParam(name = "reason", required = false) String reason,
            @RequestParam(name = "riskLevel", required = false) String riskLevel,
            @RequestParam(name = "ipAddress", required = false) String ipAddress,
            @RequestParam(name = "userAgent", required = false) String userAgent,
            @RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "to", required = false) String to,
            @RequestParam(name = "sort", required = false) String sort,
            @RequestParam(name = "direction", defaultValue = "DESC") String direction
    ) {
        var file = auditLogExportService.exportAdminActionLogs(actor, target, action, result, reason, riskLevel, ipAddress, userAgent, from, to, sort, direction);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.filename() + "\"")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(file.bytes());
    }

    // 전체 로그인 이력을 조회
    @GetMapping("/login-history")
    public PageResponseDTO<AdminLoginHistoryResponse> loginHistory(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "username", required = false) String username,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "to", required = false) String to,
            @RequestParam(name = "sort", required = false) String sort,
            @RequestParam(name = "direction", defaultValue = "DESC") String direction
    ) {
        return new PageResponseDTO<>(adminConsoleService.loginHistory(page, size, username, status, from, to, sort, direction));
    }

}
