package com.example.authapp.domain.risk.service;

import org.springframework.stereotype.Service;

import com.example.authapp.domain.audit.service.AuthEventLogService;
import com.example.authapp.domain.audit.service.SecurityIncidentService;
import com.example.authapp.domain.jwt.service.JwtService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RiskActionService {

    private final JwtService jwtService;
    private final AuthEventLogService authEventLogService;
    private final SecurityIncidentService securityIncidentService;

    public void blockHighRisk(String username, String ip, String device) {
    	// 모든 토큰 만료
    	jwtService.revokeAllByUsername(username);
    	// 위험 로그 기록
        securityIncidentService.suspiciousLogin(username, ip, device);
        // 강제 로그아웃
        authEventLogService.securityForceLogout(username, ip, device);
    }

    public void tokenReuseDetected(String username, String ip, String device) {
    	// 모든 토큰 만료
    	jwtService.revokeAllByUsername(username);
    	// 토큰 탈취 로그 기록
        securityIncidentService.tokenTheftDetected(username, ip, device);
        // 강제 로그아웃
        authEventLogService.securityForceLogout(username, ip, device);
    }
	
	
}
