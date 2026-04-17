package com.example.authapp.domain.risk.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Component;

import com.example.authapp.domain.audit.entity.LoginHistory;
import com.example.authapp.domain.audit.repository.LoginHistoryRepository;
import com.example.authapp.domain.jwt.entity.RefreshEntity;

import lombok.RequiredArgsConstructor;

//점수를 어떻게 변화시킬지만 담당
@Component
@RequiredArgsConstructor
public class RiskEvaluator {

    private final LoginHistoryRepository loginHistoryRepository;

    // =========================
    // 위험 점수 증가
    // =========================
    public int increaseScore(LoginHistory loginHistory) {
        int score = 0;
        if (!loginHistory.isSuccess()) score += 15;
        if (isNewIp(loginHistory)) score += 30;
        if (isNewDevice(loginHistory)) score += 20;
        if (isAbnormalTime(loginHistory)) score += 10;
        return score;
    }

    // =========================
    // 정상 행동 점수 감소
    // =========================
    public int decreaseScore(LoginHistory loginHistory) {
        int score = 0;
        // 기존 환경에서 성공 로그인
        if (loginHistory.isSuccess() && !isNewIp(loginHistory) && !isNewDevice(loginHistory)) {
            score -= 20;
        }
        // 최근 로그인 패턴이 안정적
        if (isConsistentPattern(loginHistory)) {
            score -= 10;
        }
        return score;
    }
    // =========================
    // 토큰 탈취 감지 (강한 증가)
    // =========================
    public int tokenRiskScore(RefreshEntity token, String currentIp, String currentDevice,String userAgent) {
        int score = 0;
        boolean ipChanged = !safeEquals(token.getIpAddress(), currentIp);
        boolean deviceChanged = !safeEquals(token.getDevice(), currentDevice);
        boolean uaChanged = !safeEquals(token.getUserAgent(), userAgent);

        boolean suspiciousIpChange = ipChanged && (deviceChanged || uaChanged);

        boolean rapidChange = false;
        if (token.getLastUsedAt() != null) {
            Duration duration = Duration.between(token.getLastUsedAt(), LocalDateTime.now());
            rapidChange = duration.toMinutes() < 5;
        }

        // 만료 토큰 재사용 
        boolean expired = token.getExpiresAt().isBefore(LocalDateTime.now());
        // 클라이언트, 네트워크 지연, 동시 요청 -> 1분 이상 지나면 탈취 판단
        boolean hardExpired = expired && Duration.between(token.getExpiresAt(), LocalDateTime.now()).toMinutes() > 1;

        if (suspiciousIpChange) score += 60;
        if (uaChanged && rapidChange) score += 50;
        if (expired) score += 80;
        if (hardExpired) score += 90;
        
        return score;
    }

    // 이 아래 지울지 결정
    // =========================
    // 토큰 탈취 감지 
    // =========================
    public boolean isTokenStolen(RefreshEntity token, String currentIp, String currentDevice, String userAgent) {
        // 토근과 요청 값 비교
    	boolean ipChanged = !safeEquals(token.getIpAddress(), currentIp);
        boolean deviceChanged = !safeEquals(token.getDevice(), currentDevice);
        boolean uaChanged = !safeEquals(token.getUserAgent(), userAgent);

        // 모바일 대응
        boolean suspiciousIpChange = ipChanged && (deviceChanged || uaChanged);

        // 빠른 변경 (세션 하이재킹)
        boolean rapidChange = false;
        if (token.getLastUsedAt() != null) {
            Duration duration = Duration.between(token.getLastUsedAt(), LocalDateTime.now());
            rapidChange = duration.toMinutes() < 5;
        }

        // 만료 토큰 재사용 
        boolean expired = token.getExpiresAt().isBefore(LocalDateTime.now());
        // 클라이언트, 네트워크 지연, 동시 요청 -> 1분 이상 지나면 탈취 판단
        boolean hardExpired = expired && Duration.between(token.getExpiresAt(), LocalDateTime.now()).toMinutes() > 1;
        
        // 최종 판단
        return suspiciousIpChange || (uaChanged && rapidChange) || hardExpired;
    }

    // =========================
    // 내부 메서드
    // =========================
    public boolean isNewIp(LoginHistory loginHistory) {
        List<LoginHistory> histories = loginHistoryRepository.findByUsername(loginHistory.getUsername());
        return histories.stream().noneMatch(h -> h.getIpAddress().equals(loginHistory.getIpAddress()));
    }

    public boolean isNewDevice(LoginHistory loginHistory) {
        List<LoginHistory> histories = loginHistoryRepository.findByUsername(loginHistory.getUsername());
        return histories.stream().noneMatch(h -> h.getDevice().equals(loginHistory.getDevice()));
    }

    public boolean isAbnormalTime(LoginHistory loginHistory) {
        int hour = loginHistory.getLoginAt().getHour();
        return (hour < 6 || hour > 23);
    }

    private boolean isConsistentPattern(LoginHistory loginHistory) {
        List<LoginHistory> histories = loginHistoryRepository.findByUsername(loginHistory.getUsername());
        return histories.stream().limit(5).allMatch(LoginHistory::isSuccess);
    }

    private boolean safeEquals(String a, String b) {
        return (a == null && b == null) || (a != null && a.equals(b));
    }
    
}
