package com.example.authapp.domain.risk.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.authapp.domain.audit.entity.LoginHistory;
import com.example.authapp.domain.jwt.entity.RefreshEntity;
import com.example.authapp.domain.risk.entity.RiskEntity;
import com.example.authapp.domain.risk.entity.RiskLevel;
import com.example.authapp.domain.risk.exception.RiskException;
import com.example.authapp.domain.risk.repository.RiskRepository;
import com.example.authapp.domain.user.entity.UserEntity;
import com.example.authapp.domain.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

// 점수 누적 + 상태 결정 + 대응
@Service
@RequiredArgsConstructor
@Transactional
public class RiskService {

    private final RiskRepository riskRepository;
    private final UserRepository userRepository;

    private final RiskFactory riskFactory;
    private final RiskEvaluator riskEvaluator;
    private final RiskEventService riskEventService;
    private final RiskActionService riskActionService;

    // =========================
    // 로그인 위험 분석
    // =========================
    public RiskEntity analyzeLoginRisk(LoginHistory loginHistory) {

        String username = loginHistory.getUsername();
        UserEntity user = userRepository.findByUsername(username).orElseThrow(RiskException::userNotFound);
    	
        // risk 테이블에 없을 경우 user가 없으면 초기화 로직 
        RiskEntity risk = riskRepository.findByUserUsername(username).orElseGet(() -> riskFactory.create(user));
        
        // 점수 계산 
        int score = riskEvaluator.increaseScore(loginHistory) + riskEvaluator.decreaseScore(loginHistory);
        
        // 리즌 빌드
        String reason = buildReason(loginHistory);

        // 위험 점수 있을 때만 저장
        if (score > 0) {
        	// 위험 이벤트 기록
            riskEventService.saveLoginRisk(loginHistory, score, reason);
            // 기존 risk 테이블에 점수 누적
            risk.increaseRisk(score, reason);
            // RiskScore 80점 이상이면 차단
            if (risk.getRiskLevel() == RiskLevel.CRITICAL) {
                riskActionService.blockHighRisk(username, loginHistory.getIpAddress(),loginHistory.getDevice());
            }
        }else if (score < 0) {
        	// 정상행동 리스크 감소
            risk.decreaseRisk(Math.abs(score), "NORMAL_LOGIN");
        }
            
        return riskRepository.save(risk);
    }

    // =========================
    // 토큰 기반 Risk 처리
    // =========================
    public boolean analyzeTokenRisk(RefreshEntity token, String ip, String device,String userAgent) {

        String username = token.getUsername();
        UserEntity user = userRepository.findByUsername(username).orElseThrow(RiskException::userNotFound);

        // risk 테이블에 없을 경우 user가 없으면 초기화 로직 
        RiskEntity risk = riskRepository.findByUserUsername(username).orElseGet(() -> riskFactory.create(user));
        
        // 1. 강제 차단 케이스 (탈취 확정)
        // revoked 토큰 재사용
        if (token.isRevoked()) {
        	// 토큰 재사용 위험 이벤트 기록
            riskEventService.saveCritical(username,"TOKEN_REUSE",ip,device);
            // 리스크 테이블에 기록
            risk.forceCritical("TOKEN_REUSE");
            // 토큰 탈취 대응
            riskActionService.tokenReuseDetected(username, ip, device);
            //throw RiskException.tokenReuseDetected();
            return false;
        }
        
        // 2. 점수 판정
        int score = riskEvaluator.tokenRiskScore(token, ip, device, userAgent);

        // 0점이면 바로 리턴
        if (score == 0) return true;
        
        // 0점 이상이면 이벤트 기록 및 risk 테이블 저장
        riskEventService.saveTokenRisk(username, score, "TOKEN_RISK", ip, device);
        risk.increaseRisk(score, "TOKEN_RISK");
        
        // 리스크 레벨별 대응 로직 -> 사용불가로 false 리턴
        if (risk.getRiskLevel() == RiskLevel.HIGH || risk.getRiskLevel() == RiskLevel.CRITICAL) {

            riskActionService.blockHighRisk(username, ip, device);

            //throw RiskException.highRiskTokenBlocked();
            return false;
        }

        riskRepository.save(risk);
        return true;
    }

    // =========================
    // 내부 메서드
    // =========================
    private String buildReason(LoginHistory loginHistory) {
        StringBuilder reason = new StringBuilder();
        if (!loginHistory.isSuccess()) reason.append("LOGIN_FAIL;");
        if (riskEvaluator.isNewIp(loginHistory)) reason.append("NEW_IP;");
        if (riskEvaluator.isNewDevice(loginHistory)) reason.append("NEW_DEVICE;");
        if (riskEvaluator.isAbnormalTime(loginHistory)) reason.append("ABNORMAL_TIME;");
        return reason.toString();
    }

}
