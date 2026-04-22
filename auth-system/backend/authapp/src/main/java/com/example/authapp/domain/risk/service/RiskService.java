package com.example.authapp.domain.risk.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.authapp.domain.audit.entity.LoginHistory;
import com.example.authapp.domain.audit.entity.SecurityEvent;
import com.example.authapp.domain.audit.entity.SecurityEventType;
import com.example.authapp.domain.audit.repository.SecurityEventRepository;
import com.example.authapp.domain.jwt.entity.RefreshEntity;
import com.example.authapp.domain.jwt.repository.RefreshRepository;
import com.example.authapp.domain.risk.entity.RiskEntity;
import com.example.authapp.domain.risk.entity.RiskEventEntity;
import com.example.authapp.domain.risk.entity.RiskLevel;
import com.example.authapp.domain.risk.repository.RiskEventRepository;
import com.example.authapp.domain.risk.repository.RiskRepository;
import com.example.authapp.domain.user.entity.UserEntity;
import com.example.authapp.domain.user.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// 점수 누적 + 상태 결정 + 대응
@Service
@RequiredArgsConstructor
@Transactional
public class RiskService {

    private final RiskRepository riskRepository;
    private final RiskEventRepository riskEventRepository;
    private final RiskEvaluator riskEvaluator;
    private final RefreshRepository refreshRepository;
    private final SecurityEventRepository securityEventRepository;
    private final UserRepository userRepository;

    // =========================
    // 로그인 위험 분석
    // =========================
    public RiskEntity analyzeLoginRisk(LoginHistory loginHistory) {

        String username = loginHistory.getUsername();
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("유저 없음"));
    	
        int increase = riskEvaluator.increaseScore(loginHistory);
        int decrease = riskEvaluator.decreaseScore(loginHistory);

        int finalScore = clamp(increase + decrease);
        String reason = buildReason(loginHistory);
        
        // 이벤트 저장 (로그)
        RiskEventEntity event = RiskEventEntity.builder()
                .username(username)
                .score(finalScore)
                .reason(reason)
                .ipAddress(loginHistory.getIpAddress())
                .device(loginHistory.getDevice())
                .location(loginHistory.getLocation())
                .loginHistory(loginHistory)
                .build();
        
        riskEventRepository.save(event);
        
        // RiskEntity 조회 or 생성
        RiskEntity risk = riskRepository.findByUserUsername(username)
                .orElseGet(() -> createNewRisk(user));
        
        // 상태 업데이트
        risk.increaseRisk(finalScore, reason);
        
        handleHighRisk(risk, username);

        return riskRepository.save(risk);
    }

    // =========================
    // 토큰 기반 Risk 처리
    // =========================
    public void analyzeTokenRisk(RefreshEntity token, String currentIp, String currentDevice,String userAgent) {

        String username = token.getUsername();

        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("유저 없음"));

        // 1. 강제 차단 케이스 (탈취 확정)
        // revoked 토큰 재사용
        if (token.isRevoked()) {
            handleTokenReuse(token, currentIp, currentDevice, "TOKEN_REUSE");
        }

        // 만료 토큰 재사용
        if (token.getExpiresAt().isBefore(java.time.LocalDateTime.now())) {
            handleTokenReuse(token, currentIp, currentDevice, "EXPIRED_TOKEN");
        }

        // 2. Risk 점수 계산
        int score = riskEvaluator.tokenRiskScore(token, currentIp, currentDevice, userAgent);

        if (score == 0) return;

        // 3. 이벤트 저장
        RiskEventEntity event = RiskEventEntity.builder()
                .username(username)
                .score(score)
                .reason("TOKEN_RISK")
                .ipAddress(currentIp)
                .device(currentDevice)
                .build();

        riskEventRepository.save(event);

        // 4. RiskEntity 업데이트
        RiskEntity risk = riskRepository.findByUserUsername(username)
                .orElseGet(() -> createNewRisk(user));

        risk.increaseRisk(score, "TOKEN_RISK");

        riskRepository.save(risk);

        // 5. HIGH 이상 차단
        if (risk.getRiskLevel() == RiskLevel.CRITICAL || risk.getRiskLevel() == RiskLevel.HIGH) {

            revokeAllUserTokens(username);

            securityEventRepository.save(
                    SecurityEvent.builder()
                            .username(username)
                            .type(SecurityEventType.TOKEN_THEFT_DETECTED)
                            .description("High risk token detected")
                            .ipAddress(currentIp)
                            .device(currentDevice)
                            .build()
            );

            throw new RuntimeException("HIGH_RISK_TOKEN_BLOCKED");
    
        }
    }

 
    // =========================
    // 내부 로직
    // =========================
    
    private void handleTokenReuse(RefreshEntity token, String ip, String device, String reason) {

        String username = token.getUsername();

        // 1. Risk 이벤트 저장 (강제 최고점)
        RiskEventEntity event = RiskEventEntity.builder()
                .username(username)
                .score(100)
                .reason(reason)
                .ipAddress(ip)
                .device(device)
                .build();

        riskEventRepository.save(event);

        // 2. RiskEntity 강제 업데이트
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("유저 없음"));

        RiskEntity risk = riskRepository.findByUserUsername(username)
                .orElseGet(() -> createNewRisk(user));

        // 강제 CRITICAL 처리
        risk.forceCritical(reason);

        riskRepository.save(risk);

        // 3. 모든 세션 종료
        revokeAllUserTokens(username);

        // 4. 보안 이벤트 기록
        securityEventRepository.save(
                SecurityEvent.builder()
                        .username(username)
                        .type(SecurityEventType.TOKEN_THEFT_DETECTED)
                        .description(reason)
                        .ipAddress(ip)
                        .device(device)
                        .build()
        );

        // 5. 즉시 차단
        throw new RuntimeException(reason);
    }
    
    
    private RiskEntity createNewRisk(UserEntity user) {
        return RiskEntity.builder()
                .user(user)
                .username(user.getUsername())
                .riskScore(0)
                .riskLevel(RiskLevel.LOW)
                .lastReason("INIT")
                .build();
    }
    
    
    private void handleHighRisk(RiskEntity risk, String username) {
        if (risk.getRiskLevel() == RiskLevel.CRITICAL) {
            revokeAllUserTokens(username);

            securityEventRepository.save(
                    SecurityEvent.builder()
                            .username(username)
                            .type(SecurityEventType.SUSPICIOUS_LOGIN)
                            .description("의심 로그인 (CRITICAL)")
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

    private String buildReason(LoginHistory loginHistory) {
        StringBuilder reason = new StringBuilder();
        if (!loginHistory.isSuccess()) reason.append("LOGIN_FAIL;");
        if (riskEvaluator.isNewIp(loginHistory)) reason.append("NEW_IP;");
        if (riskEvaluator.isNewDevice(loginHistory)) reason.append("NEW_DEVICE;");
        if (riskEvaluator.isAbnormalTime(loginHistory)) reason.append("ABNORMAL_TIME;");
        return reason.toString();
    }

}
