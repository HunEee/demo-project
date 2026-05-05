package com.example.authapp.domain.audit.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.example.authapp.domain.audit.entity.SecurityIncidentEntity;
import com.example.authapp.domain.audit.entity.SecurityIncidentType;
import com.example.authapp.domain.audit.entity.Severity;
import com.example.authapp.domain.audit.repository.SecurityIncidentRepository;
import com.example.authapp.util.ClientUtil;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class SecurityIncidentService {

    private final SecurityIncidentRepository securityIncidentrepository;

    // 공통 저장
    public void create(String username, SecurityIncidentType type, Severity severity, String description) {
        SecurityIncidentEntity incident = SecurityIncidentEntity.builder()
                .username(username)
                .type(type)
                .severity(severity)
                .description(description)
                .ipAddress(getIp())
                .device(getDevice())
                .build();
        securityIncidentrepository.save(incident);
    }

    // ==================================================================================================================
    // request 내부에서 직접 가져오기
    // ==================================================================================================================
    private HttpServletRequest getRequest() {
        ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attr != null ? attr.getRequest() : null;
    }

    private String getIp() {
        HttpServletRequest request = getRequest();
        return request != null ? ClientUtil.getIp(request) : "UNKNOWN";
    }

    private String getDevice() {
        HttpServletRequest request = getRequest();
        if (request == null) return "UNKNOWN";
        String ua = ClientUtil.getUserAgent(request);
        return ClientUtil.getDevice(ua);
    }
      
    // =====================================================
    // 편의 메서드
    // =====================================================

    public void suspiciousLogin(String username) {
        create(username, SecurityIncidentType.SUSPICIOUS_LOGIN, Severity.HIGH, "의심스러운 로그인 감지");
    }

    public void tokenTheftDetected(String username) {
        create(username, SecurityIncidentType.TOKEN_THEFT_DETECTED, Severity.CRITICAL, "토큰 탈취 의심");
    }

    public void bruteForceAttack(String username) {
        create(username, SecurityIncidentType.BRUTE_FORCE_ATTACK, Severity.HIGH, "로그인 반복 실패 감지");
    }

    public void impossibleTravel(String username) {
        create(username, SecurityIncidentType.IMPOSSIBLE_TRAVEL, Severity.MEDIUM, "비정상 지역 이동 로그인 감지");
    }

    public void abnormalSession(String username) {
        create(username, SecurityIncidentType.ABNORMAL_SESSION_ACTIVITY, Severity.MEDIUM, "비정상 세션 활동 감지" );
    }

    // 관리자 조치 완료
    public void resolve(Long incidentId, String adminUsername) {
        SecurityIncidentEntity incident = securityIncidentrepository.findById(incidentId).orElseThrow();
        incident.resolve(adminUsername);
    }
    
}