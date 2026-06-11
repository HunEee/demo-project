package com.example.authapp.domain.risk.service;

import org.springframework.stereotype.Service;

import com.example.authapp.domain.audit.entity.AdminActionType;
import com.example.authapp.domain.audit.service.AdminActionLogRequest;
import com.example.authapp.domain.audit.service.AdminActionLogService;
import com.example.authapp.domain.audit.service.AuthEventLogService;
import com.example.authapp.domain.audit.service.SecurityIncidentService;
import com.example.authapp.domain.jwt.service.RefreshTokenService;
import com.example.authapp.domain.risk.entity.RiskActionLogEntity;
import com.example.authapp.domain.risk.repository.RiskActionLogRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RiskActionService {

    private final RefreshTokenService refreshTokenService;
    private final AuthEventLogService authEventLogService;
    private final SecurityIncidentService securityIncidentService;
    private final RiskActionLogRepository riskActionLogRepository;
    private final AdminActionLogService adminActionLogService;

    public void blockHighRisk(String username, String ip, String device) {
        refreshTokenService.revokeAllByUsername(username);
        securityIncidentService.suspiciousLogin(username);
        authEventLogService.securityForceLogout(username);
        recordAuto(username, "HIGH", "BLOCK_HIGH_RISK", "SUCCESS", "High risk response executed", ip, device);
    }

    public void tokenReuseDetected(String username, String ip, String device) {
        refreshTokenService.revokeAllByUsername(username);
        securityIncidentService.tokenTheftDetected(username);
        authEventLogService.securityForceLogout(username);
        recordAuto(username, "CRITICAL", "TOKEN_REUSE_DETECTED", "SUCCESS", "Token reuse response executed", ip, device);
    }

    private void recordAuto(String username, String riskLevel, String action, String status, String reason, String ip, String device) {
        riskActionLogRepository.save(RiskActionLogEntity.builder()
                .username(username)
                .riskLevel(riskLevel)
                .action(action)
                .mode("AUTO")
                .status(status)
                .reason(reason)
                .actorUsername("SYSTEM")
                .ipAddress(ip)
                .device(device)
                .build());

        adminActionLogService.record(AdminActionLogRequest.builder()
                .actorUsername("SYSTEM")
                .targetType("USER")
                .targetId(username)
                .targetUsername(username)
                .targetName(username)
                .actionType(AdminActionType.RISK_AUTO_RESPONSE)
                .reason(reason)
                .afterValue("{\"action\":\"" + action + "\",\"status\":\"" + status + "\"}")
                .ipAddress(ip)
                .device(device)
                .result(status)
                .riskLevel(riskLevel)
                .build());
    }
}
