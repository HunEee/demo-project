package com.example.authapp.domain.audit.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.example.authapp.domain.audit.entity.LoginHistory;
import com.example.authapp.domain.audit.entity.LoginStatus;
import com.example.authapp.domain.audit.repository.LoginHistoryRepository;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LoginHistoryService {

    private final LoginHistoryRepository loginHistoryRepository;

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
        return loginHistoryRepository.save(history);
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
        return loginHistoryRepository.save(history);
    }
    
    
    // 로그아웃
    @Transactional
    public void logout(Long loginHistoryId) {
        LoginHistory history = loginHistoryRepository.findById(loginHistoryId)
                .orElseThrow(() -> new IllegalArgumentException("로그인 이력 없음"));

        history.logout(); // logoutAt + status 변경
    }
    
    
    // =========================
    // 세션 관련 메서드
    // =========================
    
    // 세션 시작 시간 조회
    @Transactional(readOnly = true)
    public LocalDateTime getSessionStartTime(Long loginHistoryId) {
        return loginHistoryRepository.findById(loginHistoryId)
                .map(LoginHistory::getLoginAt)
                .orElse(null);
    }
    
    // 세션종료 전체
    @Transactional
    public void expireAll(String username) {
        loginHistoryRepository.findByUsername(username).forEach(LoginHistory::expire);
    }
    
    
}