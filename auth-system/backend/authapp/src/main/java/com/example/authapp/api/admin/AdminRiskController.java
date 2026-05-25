package com.example.authapp.api.admin;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.authapp.domain.admin.AdminConsoleService;
import com.example.authapp.domain.admin.dto.AdminRiskResponse;
import com.example.authapp.domain.audit.dto.PageResponseDTO;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/risks")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminRiskController {

    private final AdminConsoleService adminConsoleService;

    // 위험 점수와 위험 레벨 기준으로 사용자 위험 목록을 조회
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
    
}
