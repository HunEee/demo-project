package com.example.authapp.domain.risk.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.authapp.domain.audit.entity.SecurityEventType;
import com.example.authapp.domain.audit.service.SecurityEventService;
import com.example.authapp.domain.jwt.entity.RefreshEntity;
import com.example.authapp.domain.jwt.repository.RefreshRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RiskActionService {

    private final RefreshRepository refreshRepository;
    private final SecurityEventService securityEventService;

    public void revokeAllTokens(String username) {
        List<RefreshEntity> tokens = refreshRepository.findByUsername(username);
        for (RefreshEntity token : tokens) {
            token.revoke();
        }
    }

    public void blockHighRisk(String username, String ip, String device) {

        revokeAllTokens(username);

        securityEventService.save(username, SecurityEventType.TOKEN_THEFT_DETECTED, "HIGH_RISK_BLOCKED", ip, device);
    }

    public void tokenReuseDetected(String username, String ip, String device) {

        revokeAllTokens(username);

        securityEventService.save(username, SecurityEventType.TOKEN_THEFT_DETECTED, "TOKEN_REUSE_DETECTED", ip, device);
    }
	
	
}
