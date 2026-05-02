package com.example.authapp.domain.risk.service;

import org.springframework.stereotype.Service;

import com.example.authapp.domain.audit.entity.LoginHistoryEntity;
import com.example.authapp.domain.risk.entity.RiskEventEntity;
import com.example.authapp.domain.risk.repository.RiskEventRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RiskEventService {

    private final RiskEventRepository riskEventRepository;

    public void saveLoginRisk(LoginHistoryEntity history, int score, String reason) {

        RiskEventEntity event = RiskEventEntity.builder()
                .username(history.getUsername())
                .score(score)
                .reason(reason)
                .ipAddress(history.getIpAddress())
                .device(history.getDevice())
                .location(history.getLocation())
                .loginHistory(history)
                .build();

        riskEventRepository.save(event);
    }

    public void saveTokenRisk(String username, int score, String reason, String ip, String device) {

        RiskEventEntity event = RiskEventEntity.builder()
                .username(username)
                .score(score)
                .reason(reason)
                .ipAddress(ip)
                .device(device)
                .build();

        riskEventRepository.save(event);
    }

    public void saveCritical(String username, String reason, String ip, String device) {
        saveTokenRisk(username, 100, reason, ip, device);
    }
    
    
}