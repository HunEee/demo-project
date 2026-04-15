package com.example.authapp.domain.audit.service;

import org.springframework.stereotype.Service;

import com.example.authapp.domain.audit.entity.LoginHistory;
import com.example.authapp.domain.audit.entity.LoginStatus;
import com.example.authapp.domain.audit.repository.LoginHistoryRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LoginHistoryService {

    private final LoginHistoryRepository repository;

    // 로그인 성공 히스토리 저장
    public LoginHistory saveSuccess(String username, String ip, String userAgent, String device) {
        LoginHistory history = LoginHistory.builder()
                .username(username)
                .success(true)
                .ipAddress(ip)
                .userAgent(userAgent)
                .device(device)
                .status(LoginStatus.SUCCESS)
                .build();
        return repository.save(history);
    }
    
    // 로그인 실패 히스토리 저장
    public LoginHistory saveFail(String username, String ip, String userAgent, String device, String reason) {
        LoginHistory history = LoginHistory.builder()
                .username(username)
                .success(false)
                .failReason(reason)
                .ipAddress(ip)
                .userAgent(userAgent)
                .device(device)
                .status(LoginStatus.FAILED)
                .build();
        return repository.save(history);
    }
    
    
    @Transactional
    public void logout(Long loginHistoryId) {
        LoginHistory history = repository.findById(loginHistoryId)
                .orElseThrow(() -> new IllegalArgumentException("로그인 이력 없음"));

        history.logout(); // logoutAt + status 변경
    }
    
    
}