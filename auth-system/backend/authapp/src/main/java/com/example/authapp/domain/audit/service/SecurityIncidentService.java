package com.example.authapp.domain.audit.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.authapp.domain.audit.entity.SecurityIncidentEntity;
import com.example.authapp.domain.audit.entity.SecurityIncidentType;
import com.example.authapp.domain.audit.entity.Severity;
import com.example.authapp.domain.audit.repository.SecurityIncidentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class SecurityIncidentService {

    private final SecurityIncidentRepository securityIncidentrepository;

    // 공통 저장
    public void create(String username, SecurityIncidentType type, Severity severity, String description, String ip, String device) {
        SecurityIncidentEntity incident = SecurityIncidentEntity.builder()
                .username(username)
                .type(type)
                .severity(severity)
                .description(description)
                .ipAddress(ip)
                .device(device)
                .build();
        securityIncidentrepository.save(incident);
    }

    // =====================================================
    // 편의 메서드
    // =====================================================

    public void suspiciousLogin(String username, String ip, String device) {
        create(username, SecurityIncidentType.SUSPICIOUS_LOGIN, Severity.HIGH, "의심스러운 로그인 감지", ip, device);
    }

    public void tokenTheftDetected(String username, String ip, String device) {
        create(username, SecurityIncidentType.TOKEN_THEFT_DETECTED, Severity.CRITICAL, "토큰 탈취 의심", ip, device);
    }

    public void bruteForceAttack(String username, String ip, String device) {
        create(username, SecurityIncidentType.BRUTE_FORCE_ATTACK, Severity.HIGH, "로그인 반복 실패 감지", ip, device);
    }

    public void impossibleTravel(String username, String ip, String device) {
        create(username, SecurityIncidentType.IMPOSSIBLE_TRAVEL, Severity.MEDIUM, "비정상 지역 이동 로그인 감지", ip, device);
    }

    public void abnormalSession(String username, String ip, String device) {
        create(username, SecurityIncidentType.ABNORMAL_SESSION_ACTIVITY, Severity.MEDIUM, "비정상 세션 활동 감지", ip, device);
    }

    // 관리자 조치 완료
    public void resolve(Long incidentId, String adminUsername) {
        SecurityIncidentEntity incident = securityIncidentrepository.findById(incidentId).orElseThrow();
        incident.resolve(adminUsername);
    }
    
}