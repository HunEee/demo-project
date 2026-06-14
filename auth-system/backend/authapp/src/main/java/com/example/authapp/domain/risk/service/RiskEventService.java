package com.example.authapp.domain.risk.service;

import org.springframework.stereotype.Service;

import com.example.authapp.domain.audit.entity.LoginHistoryEntity;
import com.example.authapp.domain.risk.entity.RiskEventEntity;
import com.example.authapp.domain.risk.entity.RiskEventType;
import com.example.authapp.domain.risk.entity.RiskLevel;
import com.example.authapp.domain.risk.repository.RiskEventRepository;
import com.example.authapp.domain.user.entity.UserEntity;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RiskEventService {

    private final RiskEventRepository riskEventRepository;

    public void saveLoginRisk(LoginHistoryEntity history, int score, String reason) {

        RiskEventEntity event = RiskEventEntity.builder()
                .username(history.getUsername())
                .eventType(RiskEventType.LOGIN_RISK)
                .riskLevel(levelFromScore(score))
                .score(score)
                .reason(reason)
                .description(reason)
                .ipAddress(history.getIpAddress())
                .userAgent(history.getUserAgent())
                .device(history.getDevice())
                .location(history.getLocation())
                .loginHistory(history)
                .build();

        riskEventRepository.save(event);
    }

    public void saveTokenRisk(String username, int score, String reason, String ip, String device) {

        RiskEventEntity event = RiskEventEntity.builder()
                .username(username)
                .eventType("TOKEN_REUSE".equals(reason) ? RiskEventType.TOKEN_REUSE : RiskEventType.TOKEN_RISK)
                .riskLevel(levelFromScore(score))
                .score(score)
                .reason(reason)
                .description(reason)
                .ipAddress(ip)
                .device(device)
                .build();

        riskEventRepository.save(event);
    }

    public void saveCritical(String username, String reason, String ip, String device) {
        saveTokenRisk(username, 100, reason, ip, device);
    }

    public RiskEventEntity saveRuleEvent(
            LoginHistoryEntity history,
            UserEntity user,
            RiskEventType eventType,
            RiskLevel riskLevel,
            int score,
            String description
    ) {
        RiskEventEntity event = RiskEventEntity.builder()
                .username(history.getUsername())
                .eventType(eventType)
                .riskLevel(riskLevel)
                .score(score)
                .reason(eventType.name())
                .description(description)
                .ipAddress(history.getIpAddress())
                .userAgent(history.getUserAgent())
                .device(history.getDevice())
                .location(history.getLocation())
                .resolved(false)
                .loginHistory(history)
                .build();
        return riskEventRepository.save(event);
    }

    private RiskLevel levelFromScore(int score) {
        if (score >= 80) return RiskLevel.CRITICAL;
        if (score >= 60) return RiskLevel.HIGH;
        if (score >= 30) return RiskLevel.MEDIUM;
        return RiskLevel.LOW;
    }
    
    
}
