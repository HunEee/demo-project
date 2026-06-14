package com.example.authapp.api.admin;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.authapp.application.admin.usecase.AdminRiskUseCase;
import com.example.authapp.domain.admin.dto.AdminActionRequest;
import com.example.authapp.domain.admin.dto.AdminRiskResponse;
import com.example.authapp.domain.audit.dto.PageResponseDTO;
import com.example.authapp.domain.risk.dto.RiskEventResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/risks")
@RequiredArgsConstructor
public class AdminRiskController {

    private final AdminRiskUseCase adminConsoleService;

    // 위험 점수와 위험 등급 기준으로 사용자 위험 목록을 조회한다.
    @GetMapping
    public PageResponseDTO<AdminRiskResponse> risks(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "username", required = false) String username,
            @RequestParam(name = "level", required = false) String level,
            @RequestParam(name = "minScore", required = false) Integer minScore,
            @RequestParam(name = "sort", required = false) String sort,
            @RequestParam(name = "direction", defaultValue = "DESC") String direction
    ) {
        return new PageResponseDTO<>(adminConsoleService.risks(page, size, username, level, minScore, sort, direction));
    }

    @GetMapping("/events")
    public PageResponseDTO<RiskEventResponse> riskEvents(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "username", required = false) String username,
            @RequestParam(name = "eventType", required = false) String eventType,
            @RequestParam(name = "riskLevel", required = false) String riskLevel,
            @RequestParam(name = "resolved", required = false) Boolean resolved,
            @RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "to", required = false) String to,
            @RequestParam(name = "sort", required = false) String sort,
            @RequestParam(name = "direction", defaultValue = "DESC") String direction
    ) {
        return new PageResponseDTO<>(adminConsoleService.riskEvents(page, size, username, eventType, riskLevel, resolved, from, to, sort, direction));
    }

    @PostMapping("/events/{id}/resolve")
    public void resolveRiskEvent(@PathVariable(name = "id") Long id) {
        adminConsoleService.resolveRiskEvent(id, null);
    }

    @PostMapping("/{username}/lock")
    public void lockRiskUser(@PathVariable(name = "username") String username, @RequestBody(required = false) AdminActionRequest request) {
        adminConsoleService.lockRiskUser(username, request != null ? request.normalizedReason() : null);
    }

    @PostMapping("/{username}/tokens/revoke")
    public void revokeRiskUserTokens(@PathVariable(name = "username") String username, @RequestBody(required = false) AdminActionRequest request) {
        adminConsoleService.revokeRiskUserTokens(username, request != null ? request.normalizedReason() : null);
    }

    @PostMapping("/{username}/mfa/require")
    public void requireRiskUserMfa(@PathVariable(name = "username") String username, @RequestBody(required = false) AdminActionRequest request) {
        adminConsoleService.requireRiskUserMfa(username, request != null ? request.normalizedReason() : null);
    }
    
}
