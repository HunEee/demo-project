package com.example.authapp.domain.risk.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Component;

import com.example.authapp.domain.audit.entity.LoginHistoryEntity;
import com.example.authapp.domain.audit.repository.LoginHistoryRepository;
import com.example.authapp.domain.jwt.entity.RefreshTokenEntity;

import lombok.RequiredArgsConstructor;

//점수를 어떻게 변화시킬지만 담당
@Component
@RequiredArgsConstructor
public class RiskEvaluator {

    // =========================
    // 위험 점수 증가
    // =========================
    public int increaseScore(LoginHistoryEntity loginHistory, List<LoginHistoryEntity> histories) {
        int score = 0;
        if (!loginHistory.isSuccess()) score += 15;
        if (isNewIp(loginHistory, histories)) score += 3;
        if (isNewDevice(loginHistory, histories)) score += 5;
        if (isAbnormalTime(loginHistory)) score += 2;
        return score;
    }

    // =========================
    // 정상 행동 점수 감소
    // =========================
    public int decreaseScore(LoginHistoryEntity loginHistory, List<LoginHistoryEntity> histories) {
    	
    	// 로그인 실패 시 0점 리턴
    	if (!loginHistory.isSuccess()) return 0;
    	
        int score = 0;
        
        // 기존 환경에서 성공 로그인
        if (loginHistory.isSuccess() && !isNewIp(loginHistory, histories) && !isNewDevice(loginHistory, histories)) {
            score -= 20;
        }
        
        // 최근 로그인 패턴이 안정적
        if (isConsistentPattern(histories)) {
            score -= 10;
        }
        
        return score;
    }
    
    // =========================
    // 토큰 탈취 감지 
    // =========================
    public int tokenRiskScore(RefreshTokenEntity token, String currentIp, String currentDevice,String userAgent) {
        int score = 0;
        // 토근과 요청 값 비교
        boolean ipChanged = !safeEquals(token.getIpAddress(), currentIp);
        boolean deviceChanged = !safeEquals(token.getDevice(), currentDevice);
        boolean uaChanged = !safeEquals(token.getUserAgent(), userAgent);
        
        // 모바일 대응
        boolean suspiciousChange  = ipChanged && (deviceChanged || uaChanged);

        // 빠른 변경 (세션 하이재킹)
        boolean rapidChange = false;
        if (token.getLastUsedAt() != null) {
            Duration duration = Duration.between(token.getLastUsedAt(), LocalDateTime.now());
            rapidChange = duration.toMinutes() < 5;
        }

        // 만료
        boolean expired = token.getExpiresAt().isBefore(LocalDateTime.now());
        boolean hardExpired = expired && Duration.between(token.getExpiresAt(), LocalDateTime.now()).toMinutes() > 1;

        // revoked 된 토큰 재사용하면 탈취로 판단
        if (token.isRevoked()) {
            if (token.getReplacedByToken() != null) {
                score += 90; // 재사용 → 탈취
            } else {
                score += 40; // 로그아웃 등
            }
        }
        
        if (suspiciousChange) score += 60;
        if (uaChanged && rapidChange) score += 50;
        if (hardExpired) score += 90;
        else if (expired) score += 30;
        
        return score;
    }

    // =========================
    // 내부 메서드
    // =========================
    public boolean isNewIp(LoginHistoryEntity loginHistory, List<LoginHistoryEntity> histories) {
    	return histories.stream().noneMatch(h -> h.getIpAddress().equals(loginHistory.getIpAddress()));
	}
	
	public boolean isNewDevice(LoginHistoryEntity loginHistory, List<LoginHistoryEntity> histories) {
		return histories.stream().noneMatch(h -> h.getDevice().equals(loginHistory.getDevice()));
	}
	
	public boolean isAbnormalTime(LoginHistoryEntity loginHistory) {
		int hour = loginHistory.getLoginAt().getHour();
		return hour < 6 || hour > 23;
	}
	
	private boolean isConsistentPattern(List<LoginHistoryEntity> histories) {
		return histories.stream().limit(5).allMatch(LoginHistoryEntity::isSuccess);
	}

    private boolean safeEquals(String a, String b) {
        return (a == null && b == null) || (a != null && a.equals(b));
    }
    
}
