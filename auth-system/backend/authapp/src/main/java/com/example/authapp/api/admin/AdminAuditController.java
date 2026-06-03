package com.example.authapp.api.admin;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.authapp.domain.admin.AdminConsoleService;
import com.example.authapp.domain.admin.dto.AdminAuditLogResponse;
import com.example.authapp.domain.admin.dto.AdminLoginHistoryResponse;
import com.example.authapp.domain.audit.dto.PageResponseDTO;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminAuditController {

    private final AdminConsoleService adminConsoleService;

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
