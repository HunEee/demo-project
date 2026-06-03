package com.example.authapp.api.admin;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.authapp.domain.admin.AdminConsoleService;
import com.example.authapp.domain.admin.dto.AdminDashboardSummaryResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminConsoleService adminConsoleService;

    // 관리자 홈 화면의 핵심 운영 지표를 반환
    @GetMapping("/summary")
    public AdminDashboardSummaryResponse summary() {
        return adminConsoleService.dashboardSummary();
    }
    
}
