package com.example.authapp.api.admin;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.authapp.application.admin.usecase.AdminDashboardUseCase;
import com.example.authapp.domain.admin.dto.AdminDashboardSummaryResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardUseCase adminConsoleService;

    // 관리자 대시보드 요약 지표를 반환한다.
    @GetMapping("/summary")
    public AdminDashboardSummaryResponse summary() {
        return adminConsoleService.dashboardSummary();
    }
    
}
