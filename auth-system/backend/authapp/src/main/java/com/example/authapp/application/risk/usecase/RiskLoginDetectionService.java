package com.example.authapp.application.risk.usecase;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.example.authapp.domain.audit.entity.LoginHistoryEntity;
import com.example.authapp.domain.audit.repository.LoginHistoryRepository;
import com.example.authapp.domain.auth.lockout.entity.AccountLockoutSettingsEntity;
import com.example.authapp.domain.auth.lockout.service.AccountLockoutSettingsService;
import com.example.authapp.domain.risk.entity.RiskDetectionSettingsEntity;
import com.example.authapp.domain.risk.entity.RiskEventType;
import com.example.authapp.domain.risk.entity.RiskLevel;
import com.example.authapp.domain.risk.entity.RiskRuleSettingsEntity;
import com.example.authapp.domain.risk.service.RiskEventService;
import com.example.authapp.domain.risk.service.RiskSettingsService;
import com.example.authapp.domain.user.entity.UserEntity;
import com.example.authapp.domain.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RiskLoginDetectionService {

    private final LoginHistoryRepository loginHistoryRepository;
    private final RiskEventService riskEventService;
    private final AccountLockoutSettingsService accountLockoutSettingsService;
    private final RiskSettingsService riskSettingsService;
    private final UserRepository userRepository;

    public int detectFailure(UserEntity user, LoginHistoryEntity history) {
        if (user == null || history == null) return 0;

        int score = 0;
        if (user.isDeleted() || !user.isEnabled()) {
            score += saveConfigured(history, user, RiskEventType.DISABLED_ACCOUNT_LOGIN_ATTEMPT, RiskLevel.CRITICAL, 90, "Disabled account login attempt");
        }
        if (user.isLocked()) {
            score += saveConfigured(history, user, RiskEventType.LOCKED_ACCOUNT_LOGIN_ATTEMPT, RiskLevel.HIGH, 70, "Locked account login attempt");
        }
        if (user.getRoles().stream().anyMatch(role -> "ROLE_ADMIN".equals(role.getName()))) {
            score += saveConfigured(history, user, RiskEventType.ADMIN_LOGIN_FAILURE, RiskLevel.HIGH, 60, "Admin account login failure");
        }
        AccountLockoutSettingsEntity lockoutPolicy = accountLockoutSettingsService.current();
        int threshold = failureThreshold(user, lockoutPolicy);
        long recentFailures = loginHistoryRepository.countFailedLoginsSince(
                history.getUsername(),
                LocalDateTime.now().minusMinutes(lockoutPolicy.getFailureWindowMinutes())
        );
        if (recentFailures == threshold) {
            score += saveConfigured(history, user, RiskEventType.LOGIN_FAILURE_BURST, RiskLevel.HIGH, 60,
                    "At least " + threshold + " login failures within " + lockoutPolicy.getFailureWindowMinutes() + " minutes");
        }
        if (recentFailures >= threshold) {
            lockUserIfNeeded(user);
        }
        return score;
    }

    public int detectSuccess(UserEntity user, LoginHistoryEntity history) {
        return detectSuccess(user, history, LocalDateTime.now());
    }

    public int detectSuccess(UserEntity user, LoginHistoryEntity history, LocalDateTime currentTime) {
        if (user == null || history == null) return 0;

        int score = 0;
        Long historyId = history.getId() == null ? -1L : history.getId();
        RiskDetectionSettingsEntity policy = riskSettingsService.currentDetection();
        long previousSuccessCount = loginHistoryRepository.countSuccessfulLoginsBefore(history.getUsername(), historyId);
        boolean firstSuccessfulLogin = previousSuccessCount == 0;
        if (hasText(history.getIpAddress())
                && !(firstSuccessfulLogin && policy.isFirstLoginNewIpExempt())
                && !loginHistoryRepository.existsSuccessfulLoginFromIp(history.getUsername(), history.getIpAddress(), historyId)) {
            score += saveConfigured(history, user, RiskEventType.NEW_IP_LOGIN, RiskLevel.MEDIUM, 25, "New IP login");
        }
        if (hasText(history.getUserAgent())
                && !(firstSuccessfulLogin && policy.isFirstLoginNewUserAgentExempt())
                && !loginHistoryRepository.existsSuccessfulLoginFromUserAgent(history.getUsername(), history.getUserAgent(), historyId)) {
            score += saveConfigured(history, user, RiskEventType.NEW_USER_AGENT_LOGIN, RiskLevel.MEDIUM, 25, "New User-Agent login");
        }
        int hour = currentTime == null ? LocalDateTime.now().getHour() : currentTime.getHour();
        RiskRuleSettingsEntity nightRule = riskSettingsService.findRule(RiskEventType.NIGHT_LOGIN).orElse(null);
        int nightStart = nightRule == null || nightRule.getNightStartHour() == null ? 0 : nightRule.getNightStartHour();
        int nightEnd = nightRule == null || nightRule.getNightEndHour() == null ? 6 : nightRule.getNightEndHour();
        if (isNightHour(hour, nightStart, nightEnd)) {
            score += saveConfigured(history, user, RiskEventType.NIGHT_LOGIN, RiskLevel.LOW, 10, "Night login");
        }
        return score;
    }

    private int saveConfigured(
            LoginHistoryEntity history,
            UserEntity user,
            RiskEventType eventType,
            RiskLevel fallbackRiskLevel,
            int fallbackScore,
            String fallbackDescription
    ) {
        RiskRuleSettingsEntity rule = riskSettingsService.findRule(eventType).orElse(null);
        if (rule != null && !rule.isEnabled()) {
            return 0;
        }
        RiskLevel riskLevel = rule == null ? fallbackRiskLevel : rule.getRiskLevel();
        int score = rule == null ? fallbackScore : rule.getScore();
        String description = rule == null || !hasText(rule.getDescription()) ? fallbackDescription : rule.getDescription();
        return save(history, user, eventType, riskLevel, score, description);
    }

    private int save(
            LoginHistoryEntity history,
            UserEntity user,
            RiskEventType eventType,
            RiskLevel riskLevel,
            int score,
            String description
    ) {
        riskEventService.saveRuleEvent(history, user, eventType, riskLevel, score, description);
        return score;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private int failureThreshold(UserEntity user, AccountLockoutSettingsEntity lockoutPolicy) {
        boolean admin = user.getRoles().stream().anyMatch(role -> "ROLE_ADMIN".equals(role.getName()));
        return admin ? lockoutPolicy.getAdminFailedLoginThreshold() : lockoutPolicy.getFailedLoginThreshold();
    }

    private void lockUserIfNeeded(UserEntity user) {
        if (user.isLocked()) {
            return;
        }
        user.lock();
        userRepository.save(user);
    }

    private boolean isNightHour(int hour, int start, int end) {
        if (start == end) {
            return false;
        }
        if (start < end) {
            return hour >= start && hour < end;
        }
        return hour >= start || hour < end;
    }
}
