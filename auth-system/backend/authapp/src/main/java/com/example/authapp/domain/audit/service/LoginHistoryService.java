package com.example.authapp.domain.audit.service;

import org.springframework.stereotype.Service;

import com.example.authapp.domain.audit.entity.LoginHistory;
import com.example.authapp.domain.audit.entity.LoginStatus;
import com.example.authapp.domain.audit.repository.LoginHistoryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LoginHistoryService {

    private final LoginHistoryRepository repository;

    public LoginHistory saveSuccess(
            String username,
            String ip,
            String userAgent,
            String device
    ) {
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
    
    public void logout(Long loginHistoryId) {
        LoginHistory history = repository.findById(loginHistoryId)
                .orElseThrow(() -> new IllegalArgumentException("로그인 이력 없음"));

        history.logout(); // logoutAt + status 변경
    }
    
    
}