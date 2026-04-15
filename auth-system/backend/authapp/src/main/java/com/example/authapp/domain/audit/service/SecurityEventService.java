package com.example.authapp.domain.audit.service;

import org.springframework.stereotype.Service;

import com.example.authapp.domain.audit.entity.SecurityEvent;
import com.example.authapp.domain.audit.entity.SecurityEventType;
import com.example.authapp.domain.audit.repository.SecurityEventRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SecurityEventService {

    private final SecurityEventRepository repository;

    // 공통 저장
    public void save(String username,SecurityEventType type,String description,String ip,String device) {
        SecurityEvent event = SecurityEvent.builder()
                .username(username)
                .type(type)
                .description(description)
                .ipAddress(ip)
                .device(device)
                .build();

        repository.save(event);
    }

    //********************************************************************************
    // 편의 메서드
    //********************************************************************************

    public void loginSuccess(String username, String ip, String device) {
        save(username, SecurityEventType.LOGIN_SUCCESS, "로그인 성공", ip, device);
    }

    public void loginFail(String username, String ip, String device, String reason) {
        save(username, SecurityEventType.LOGIN_FAIL, "로그인 실패: " + reason, ip, device);
    }

    public void logout(String username, String ip, String device) {
        save(username, SecurityEventType.LOGOUT, "로그아웃", ip, device);
    }

    public void passwordChange(String username, String ip, String device) {
        save(username, SecurityEventType.PASSWORD_CHANGE, "비밀번호 변경", ip, device);
    }

    public void tokenReissue(String username, String ip, String device) {
        save(username, SecurityEventType.TOKEN_REISSUE, "토큰 재발급", ip, device);
    }

    public void suspiciousLogin(String username, String ip, String device) {
        save(username, SecurityEventType.SUSPICIOUS_LOGIN, "의심스러운 로그인 감지", ip, device);
    }
}