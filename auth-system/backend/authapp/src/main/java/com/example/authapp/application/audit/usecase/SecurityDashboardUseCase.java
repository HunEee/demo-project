package com.example.authapp.application.audit.usecase;

import org.springframework.stereotype.Service;

import com.example.authapp.domain.audit.dto.SecurityStatusResponseDTO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SecurityDashboardUseCase {

    private final SecurityDashboardService securityDashboardService;

    public SecurityStatusResponseDTO getSecurityStatus(String username) {
        return securityDashboardService.getSecurityStatus(username);
    }
}

