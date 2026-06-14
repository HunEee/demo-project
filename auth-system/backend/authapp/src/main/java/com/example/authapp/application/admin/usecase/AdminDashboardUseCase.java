package com.example.authapp.application.admin.usecase;

import org.springframework.stereotype.Service;

import com.example.authapp.domain.admin.dto.AdminDashboardSummaryResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminDashboardUseCase {

    private final AdminConsoleUseCase adminConsoleUseCase;

    public AdminDashboardSummaryResponse dashboardSummary() {
        return adminConsoleUseCase.dashboardSummary();
    }
}
