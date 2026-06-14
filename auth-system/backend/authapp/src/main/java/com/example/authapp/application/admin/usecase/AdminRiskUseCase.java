package com.example.authapp.application.admin.usecase;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import com.example.authapp.domain.admin.dto.AdminRiskResponse;
import com.example.authapp.domain.risk.dto.RiskEventResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminRiskUseCase {

    private final AdminConsoleUseCase adminConsoleUseCase;

    public Page<AdminRiskResponse> risks(int page, int size, String username, String level, Integer minScore, String sort, String direction) {
        return adminConsoleUseCase.risks(page, size, username, level, minScore, sort, direction);
    }

    public Page<RiskEventResponse> riskEvents(int page, int size, String username, String eventType, String riskLevel, Boolean resolved, String from, String to, String sort, String direction) {
        return adminConsoleUseCase.riskEvents(page, size, username, eventType, riskLevel, resolved, from, to, sort, direction);
    }

    public void resolveRiskEvent(Long id, String actorUsername) {
        adminConsoleUseCase.resolveRiskEvent(id, actorUsername);
    }

    public void lockRiskUser(String username, String reason) {
        adminConsoleUseCase.lockRiskUser(username, reason);
    }

    public void revokeRiskUserTokens(String username, String reason) {
        adminConsoleUseCase.revokeRiskUserTokens(username, reason);
    }

    public void requireRiskUserMfa(String username, String reason) {
        adminConsoleUseCase.requireRiskUserMfa(username, reason);
    }
}
