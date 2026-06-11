package com.example.authapp.api.admin;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.authapp.domain.admin.AdminConsoleService;
import com.example.authapp.domain.admin.dto.AdminActionRequest;
import com.example.authapp.domain.admin.dto.AdminAuditLogResponse;
import com.example.authapp.domain.admin.dto.AdminIncidentResponse;
import com.example.authapp.domain.audit.dto.PageResponseDTO;
import com.example.authapp.security.principal.UserPrincipal;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminSecurityController {

    private final AdminConsoleService adminConsoleService;

    // 보안 이벤트 중심 목록
    // 감사 로그와 같은 저장소를 사용하되 메뉴를 분리
    @GetMapping("/security-events")
    public PageResponseDTO<AdminAuditLogResponse> securityEvents(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "username", required = false) String username,
            @RequestParam(name = "type", required = false) String type,
            @RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "to", required = false) String to,
            @RequestParam(name = "sort", required = false) String sort,
            @RequestParam(name = "direction", defaultValue = "DESC") String direction
    ) {
        return new PageResponseDTO<>(adminConsoleService.securityEvents(page, size, username, type, from, to, sort, direction));
    }

    // 보안 사고 목록을 조회
    @GetMapping("/incidents")
    public PageResponseDTO<AdminIncidentResponse> incidents(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "username", required = false) String username,
            @RequestParam(name = "type", required = false) String type,
            @RequestParam(name = "severity", required = false) String severity,
            @RequestParam(name = "resolved", required = false) Boolean resolved,
            @RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "to", required = false) String to,
            @RequestParam(name = "sort", required = false) String sort,
            @RequestParam(name = "direction", defaultValue = "DESC") String direction
    ) {
        return new PageResponseDTO<>(adminConsoleService.incidents(page, size, username, type, severity, resolved, from, to, sort, direction));
    }

    // 사고를 해결 처리
    @PostMapping("/incidents/{id}/resolve")
    public void resolveIncident(
            @PathVariable Long id,
            @RequestBody(required = false) AdminActionRequest request,
            @AuthenticationPrincipal UserPrincipal admin
    ) {
        adminConsoleService.resolveIncident(id, admin.getUsername(), request != null ? request.normalizedReason() : null);
    }

}
