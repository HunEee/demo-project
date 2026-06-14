package com.example.authapp.domain.risk.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Component;

import com.example.authapp.domain.audit.entity.LoginHistoryEntity;
import com.example.authapp.domain.jwt.entity.RefreshTokenEntity;
import com.example.authapp.domain.risk.entity.RiskEventType;
import com.example.authapp.domain.risk.entity.RiskRuleSettingsEntity;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RiskEvaluator {

    private final RiskSettingsService riskSettingsService;

    public int increaseScore(LoginHistoryEntity loginHistory, List<LoginHistoryEntity> histories) {
        int score = 0;
        if (!loginHistory.isSuccess()) score += scoreFor(RiskEventType.LOGIN_RISK, 15);
        if (isNewIp(loginHistory, histories)) score += scoreFor(RiskEventType.NEW_IP_LOGIN, 3);
        if (isNewDevice(loginHistory, histories)) score += scoreFor(RiskEventType.NEW_USER_AGENT_LOGIN, 5);
        if (isAbnormalTime(loginHistory)) score += scoreFor(RiskEventType.NIGHT_LOGIN, 2);
        return score;
    }

    public int decreaseScore(LoginHistoryEntity loginHistory, List<LoginHistoryEntity> histories) {
        if (!loginHistory.isSuccess()) return 0;

        int score = 0;
        if (loginHistory.isSuccess() && !isNewIp(loginHistory, histories) && !isNewDevice(loginHistory, histories)) {
            score -= 20;
        }
        if (isConsistentPattern(histories)) {
            score -= 10;
        }
        return score;
    }

    public int tokenRiskScore(RefreshTokenEntity token, String currentIp, String currentDevice, String userAgent) {
        int score = 0;
        boolean ipChanged = !safeEquals(token.getIpAddress(), currentIp);
        boolean deviceChanged = !safeEquals(token.getDevice(), currentDevice);
        boolean uaChanged = !safeEquals(token.getUserAgent(), userAgent);
        boolean suspiciousChange = ipChanged && (deviceChanged || uaChanged);

        boolean rapidChange = false;
        if (token.getLastUsedAt() != null) {
            Duration duration = Duration.between(token.getLastUsedAt(), LocalDateTime.now());
            rapidChange = duration.toMinutes() < 5;
        }

        boolean expired = token.getExpiresAt().isBefore(LocalDateTime.now());
        boolean hardExpired = expired && Duration.between(token.getExpiresAt(), LocalDateTime.now()).toMinutes() > 1;

        if (token.isRevoked()) {
            if (token.getReplacedByToken() != null) {
                score += scoreFor(RiskEventType.TOKEN_REUSE, 90);
            } else {
                score += scoreFor(RiskEventType.TOKEN_RISK, 40);
            }
        }

        if (suspiciousChange) score += scoreFor(RiskEventType.TOKEN_CONTEXT_CHANGED, 60);
        if (uaChanged && rapidChange) score += scoreFor(RiskEventType.TOKEN_RAPID_CONTEXT_CHANGE, 50);
        if (hardExpired) score += scoreFor(RiskEventType.TOKEN_HARD_EXPIRED_USED, 90);
        else if (expired) score += scoreFor(RiskEventType.TOKEN_RISK, 30);

        return score;
    }

    public boolean isNewIp(LoginHistoryEntity loginHistory, List<LoginHistoryEntity> histories) {
        String currentIp = loginHistory.getIpAddress();
        if (currentIp == null || currentIp.isBlank()) return false;
        return histories.stream()
                .filter(history -> loginHistory.getId() == null || !loginHistory.getId().equals(history.getId()))
                .noneMatch(h -> currentIp.equals(h.getIpAddress()));
    }

    public boolean isNewDevice(LoginHistoryEntity loginHistory, List<LoginHistoryEntity> histories) {
        String currentDevice = loginHistory.getDevice();
        if (currentDevice == null || currentDevice.isBlank()) return false;
        return histories.stream()
                .filter(history -> loginHistory.getId() == null || !loginHistory.getId().equals(history.getId()))
                .noneMatch(h -> currentDevice.equals(h.getDevice()));
    }

    public boolean isAbnormalTime(LoginHistoryEntity loginHistory) {
        int hour = loginHistory.getLoginAt().getHour();
        RiskRuleSettingsEntity rule = riskSettingsService.findRule(RiskEventType.NIGHT_LOGIN).orElse(null);
        int start = rule == null || rule.getNightStartHour() == null ? 0 : rule.getNightStartHour();
        int end = rule == null || rule.getNightEndHour() == null ? 6 : rule.getNightEndHour();
        if (start == end) return false;
        if (start < end) return hour >= start && hour < end;
        return hour >= start || hour < end;
    }

    private boolean isConsistentPattern(List<LoginHistoryEntity> histories) {
        return histories.stream().limit(5).allMatch(LoginHistoryEntity::isSuccess);
    }

    private int scoreFor(RiskEventType eventType, int fallback) {
        return riskSettingsService.findRule(eventType)
                .filter(RiskRuleSettingsEntity::isEnabled)
                .map(RiskRuleSettingsEntity::getScore)
                .orElse(fallback);
    }

    private boolean safeEquals(String a, String b) {
        return (a == null && b == null) || (a != null && a.equals(b));
    }
}
