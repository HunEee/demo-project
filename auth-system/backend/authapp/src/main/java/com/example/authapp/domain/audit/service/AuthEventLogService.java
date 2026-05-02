package com.example.authapp.domain.audit.service;

import org.springframework.stereotype.Service;

import com.example.authapp.domain.audit.entity.AuthEventLogEntity;
import com.example.authapp.domain.audit.entity.AuthEventType;
import com.example.authapp.domain.audit.repository.AuthEventLogRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthEventLogService {

    private final AuthEventLogRepository repository;

    // 공통 저장
    public void save(String username,AuthEventType type,String description,String ip,String device) {
        AuthEventLogEntity event = AuthEventLogEntity.builder()
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
        save(username, AuthEventType.LOGIN_SUCCESS, "로그인 성공", ip, device);
    }

    public void loginFail(String username, String ip, String device, String reason) {
        save(username, AuthEventType.LOGIN_FAIL, "로그인 실패: " + reason, ip, device);
    }

    public void logout(String username, String ip, String device) {
        save(username, AuthEventType.LOGOUT, "로그아웃", ip, device);
    }

    public void passwordChange(String username, String ip, String device) {
        save(username, AuthEventType.PASSWORD_CHANGE, "비밀번호 변경", ip, device);
    }

    public void tokenReissue(String username, String ip, String device) {
        save(username, AuthEventType.TOKEN_REISSUE, "토큰 재발급", ip, device);
    }
    
    
    public void adminForceLogout(String username, String ip, String device) {
        save(username, AuthEventType.ADMIN_FORCE_LOGOUT, "관리자 강제 로그아웃", ip,device);
    }

    public void securityForceLogout(String username, String ip, String device) {
        save(username, AuthEventType.SECURITY_FORCE_LOGOUT, "보안 정책에 의한 강제 로그아웃", ip, device);
    }
    

}