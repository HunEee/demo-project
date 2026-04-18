package com.example.authapp.domain.risk.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.authapp.domain.audit.entity.LoginHistory;
import com.example.authapp.domain.audit.entity.SecurityEvent;
import com.example.authapp.domain.audit.entity.SecurityEventType;
import com.example.authapp.domain.audit.repository.SecurityEventRepository;
import com.example.authapp.domain.jwt.entity.RefreshEntity;
import com.example.authapp.domain.jwt.repository.RefreshRepository;
import com.example.authapp.domain.risk.entity.RiskEntity;
import com.example.authapp.domain.risk.entity.RiskLevel;
import com.example.authapp.domain.risk.repository.RiskRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// 점수 누적 + 상태 결정 + 대응
@Service
@RequiredArgsConstructor
@Transactional
public class RiskService {

    private final RiskRepository riskRepository;
    private final RiskEvaluator riskEvaluator;
    private final RefreshRepository refreshRepository;
    private final SecurityEventRepository securityEventRepository;

    // =========================
    // 로그인 위험 분석
    // =========================
    public RiskEntity analyzeLoginRisk(LoginHistory loginHistory) {

        int increase = riskEvaluator.increaseScore(loginHistory);
        int decrease = riskEvaluator.decreaseScore(loginHistory);

        int finalScore = clamp(increase + decrease);

        RiskLevel level = calculateLevel(finalScore);

        RiskEntity risk = RiskEntity.builder()
                .username(loginHistory.getUsername())
                .riskScore(finalScore)
                .riskLevel(level)
                .riskReason(buildReason(loginHistory))
                .ipAddress(loginHistory.getIpAddress())
                .device(loginHistory.getDevice())
                .location(loginHistory.getLocation())
               // .loginHistory(loginHistory)
                .createdAt(LocalDateTime.now())
                .build();

        handleHighRisk(risk);

        return riskRepository.save(risk);
    }

    // =========================
    // 토큰 기반 Risk 처리
    // =========================
    public void analyzeTokenRisk(RefreshEntity token, String currentIp, String currentDevice,String userAgent) {

    	// 토큰 탈취 여부 판단
        int score = riskEvaluator.tokenRiskScore(token, currentIp, currentDevice, userAgent);
        if (score == 0) return;

        RiskLevel level = calculateLevel(score);

        // 위험 대응
        if (level == RiskLevel.CRITICAL || level == RiskLevel.HIGH) {
            token.revoke();
            revokeAllUserTokens(token.getUsername());
        }

        RiskEntity risk = RiskEntity.builder()
                .username(token.getUsername())
                .riskScore(score)
                .riskLevel(level)
                .riskReason("TOKEN_RISK")
                .ipAddress(currentIp)
                .device(currentDevice)
                .build();

        handleHighRisk(risk);

        riskRepository.save(risk);

        securityEventRepository.save(
                SecurityEvent.builder()
                        .username(token.getUsername())
                        .type((SecurityEventType.TOKEN_THEFT_DETECTED))
                        .description("Token risk detected")
                        .ipAddress(currentIp)
                        .device(currentDevice)
                        .build()
        );
    }

 
    // =========================
    // 내부 로직
    // =========================
    private void handleHighRisk(RiskEntity risk) {
        if (risk.getRiskLevel() == RiskLevel.CRITICAL) {
            securityEventRepository.save(
                    SecurityEvent.builder()
                            .username(risk.getUsername())
                            .type(SecurityEventType.SUSPICIOUS_LOGIN)
                            .description("의심 로그인")
                            .ipAddress(risk.getIpAddress())
                            .device(risk.getDevice())
                            .build()
            );
        }
    }
    
    private void revokeAllUserTokens(String username) {
        List<RefreshEntity> tokens = refreshRepository.findByUsername(username);
        for (RefreshEntity t : tokens) {
            t.revoke();
        }
    }
    
    private int clamp(int score) {
        return Math.max(0, Math.min(score, 100));
    }

    private RiskLevel calculateLevel(int score) {
        if (score >= 80) return RiskLevel.CRITICAL;
        if (score >= 60) return RiskLevel.HIGH;
        if (score >= 30) return RiskLevel.MEDIUM;
        return RiskLevel.LOW;
    }

    private String buildReason(LoginHistory loginHistory) {
        StringBuilder reason = new StringBuilder();
        if (!loginHistory.isSuccess()) reason.append("LOGIN_FAIL;");
        if (riskEvaluator.isNewIp(loginHistory)) reason.append("NEW_IP;");
        if (riskEvaluator.isNewDevice(loginHistory)) reason.append("NEW_DEVICE;");
        if (riskEvaluator.isAbnormalTime(loginHistory)) reason.append("ABNORMAL_TIME;");
        return reason.toString();
    }

}
