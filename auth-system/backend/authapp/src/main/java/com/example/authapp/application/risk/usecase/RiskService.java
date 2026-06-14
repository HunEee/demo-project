package com.example.authapp.application.risk.usecase;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.authapp.domain.audit.entity.LoginHistoryEntity;
import com.example.authapp.domain.audit.repository.LoginHistoryRepository;
import com.example.authapp.domain.jwt.entity.RefreshTokenEntity;
import com.example.authapp.domain.risk.entity.RiskEntity;
import com.example.authapp.domain.risk.entity.RiskDetectionSettingsEntity;
import com.example.authapp.domain.risk.entity.RiskLevel;
import com.example.authapp.domain.risk.exception.RiskException;
import com.example.authapp.domain.risk.repository.RiskRepository;
import com.example.authapp.domain.risk.service.RiskEvaluator;
import com.example.authapp.domain.risk.service.RiskEventService;
import com.example.authapp.domain.risk.service.RiskFactory;
import com.example.authapp.domain.risk.service.RiskSettingsService;
import com.example.authapp.domain.user.entity.UserEntity;
import com.example.authapp.domain.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class RiskService {

    private final RiskRepository riskRepository;
    private final UserRepository userRepository;
    private final LoginHistoryRepository loginHistoryRepository;
    private final RiskFactory riskFactory;
    private final RiskEvaluator riskEvaluator;
    private final RiskEventService riskEventService;
    private final RiskActionService riskActionService;
    private final RiskSettingsService riskSettingsService;

    public RiskEntity analyzeLoginRisk(UserEntity user, LoginHistoryEntity loginHistory) {
        String username = loginHistory.getUsername();
        List<LoginHistoryEntity> histories = loginHistoryRepository.findTop20ByUsernameOrderByLoginAtDesc(username);
        RiskEntity risk = riskRepository.findByUserUsername(username).orElseGet(() -> riskFactory.create(user));
        RiskDetectionSettingsEntity policy = riskSettingsService.currentDetection();

        int score = riskEvaluator.increaseScore(loginHistory, histories)
                + riskEvaluator.decreaseScore(loginHistory, histories);
        String reason = buildReason(loginHistory, histories);

        if (score > 0) {
            riskEventService.saveLoginRisk(loginHistory, score, reason);
            risk.increaseRisk(score, reason, policy.getMediumThreshold(), policy.getHighThreshold(), policy.getCriticalThreshold());
            if (policy.isAutoResponseEnabled() && risk.getRiskScore() >= policy.getLoginBlockScore()) {
                riskActionService.blockHighRisk(username, loginHistory.getIpAddress(), loginHistory.getDevice());
            }
        } else if (score < 0) {
            risk.decreaseRisk(Math.abs(score), "NORMAL_LOGIN", policy.getMediumThreshold(), policy.getHighThreshold(), policy.getCriticalThreshold());
        }

        return riskRepository.save(risk);
    }

    public RiskEntity applyLoginRuleScore(UserEntity user, LoginHistoryEntity loginHistory, int score, String reason) {
        String username = loginHistory.getUsername();
        RiskEntity risk = riskRepository.findByUserUsername(username).orElseGet(() -> riskFactory.create(user));
        RiskDetectionSettingsEntity policy = riskSettingsService.currentDetection();
        if (score > 0) {
            risk.increaseRisk(score, reason, policy.getMediumThreshold(), policy.getHighThreshold(), policy.getCriticalThreshold());
            if (policy.isAutoResponseEnabled() && risk.getRiskScore() >= policy.getLoginBlockScore()) {
                riskActionService.blockHighRisk(username, loginHistory.getIpAddress(), loginHistory.getDevice());
            }
        }
        return riskRepository.save(risk);
    }

    public boolean analyzeTokenRisk(RefreshTokenEntity token, String ip, String device, String userAgent) {
        String username = token.getUsername();
        UserEntity user = userRepository.findByUsername(username).orElseThrow(RiskException::userNotFound);
        RiskEntity risk = riskRepository.findByUserUsername(username).orElseGet(() -> riskFactory.create(user));
        RiskDetectionSettingsEntity policy = riskSettingsService.currentDetection();

        if (token.isRevoked()) {
            if (isExpectedRevocation(token.getRevokedReason())) {
                riskEventService.saveTokenRisk(username, 0, "REVOKED_SESSION_USED", ip, device);
                return false;
            }

            token.markReuseDetected(LocalDateTime.now());
            riskEventService.saveCritical(username, "TOKEN_REUSE", ip, device);
            if (policy.isTokenReuseForceCritical()) {
                risk.forceCritical("TOKEN_REUSE");
            }
            if (policy.isAutoResponseEnabled() && policy.isTokenReuseRevokeAllSessions()) {
                riskActionService.tokenReuseDetected(username, ip, device);
            }
            return false;
        }

        int score = riskEvaluator.tokenRiskScore(token, ip, device, userAgent);
        if (score == 0) {
            return true;
        }

        riskEventService.saveTokenRisk(username, score, "TOKEN_RISK", ip, device);
        risk.increaseRisk(score, "TOKEN_RISK", policy.getMediumThreshold(), policy.getHighThreshold(), policy.getCriticalThreshold());

        if (policy.isAutoResponseEnabled() && risk.getRiskScore() >= policy.getTokenRevokeScore()) {
            riskActionService.blockHighRisk(username, ip, device);
            return false;
        }

        riskRepository.save(risk);
        return true;
    }

    private String buildReason(LoginHistoryEntity loginHistory, List<LoginHistoryEntity> histories) {
        StringBuilder reason = new StringBuilder();
        if (!loginHistory.isSuccess()) {
            reason.append("LOGIN_FAIL;");
        }
        if (riskEvaluator.isNewIp(loginHistory, histories)) {
            reason.append("NEW_IP;");
        }
        if (riskEvaluator.isNewDevice(loginHistory, histories)) {
            reason.append("NEW_DEVICE;");
        }
        if (riskEvaluator.isAbnormalTime(loginHistory)) {
            reason.append("ABNORMAL_TIME;");
        }
        return reason.toString();
    }

    private boolean isExpectedRevocation(String reason) {
        if (reason == null || reason.isBlank()) {
            return false;
        }
        return switch (reason) {
            case "LOGOUT",
                    "USER_LOGOUT_OTHER_DEVICE",
                    "USER_LOGOUT_OTHER_DEVICES",
                    "ADMIN_REVOKE_SESSION",
                    "ADMIN_REVOKE_ALL_SESSIONS",
                    "ADMIN_REVOKE",
                    "ADMIN_REVOKE_ALL",
                    "PASSWORD_CHANGED",
                    "PASSWORD_RESET",
                    "ACCOUNT_DELETED",
                    "MFA_FAILED_LIMIT",
                    "HIGH_RISK_BLOCK",
                    "USER_REVOKE" -> true;
            default -> false;
        };
    }
}
