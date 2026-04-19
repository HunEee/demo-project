package com.example.authapp.domain.audit.service;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.example.authapp.domain.audit.dto.LoginHistoryResponse;
import com.example.authapp.domain.audit.dto.LoginHistoryResponseDTO;
import com.example.authapp.domain.audit.entity.LoginHistory;
import com.example.authapp.domain.audit.entity.LoginStatus;
import com.example.authapp.domain.audit.mapper.LoginHistoryMapper;
import com.example.authapp.domain.audit.repository.LoginHistoryRepository;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LoginHistoryService {

    private final LoginHistoryRepository loginHistoryRepository;

    // 로그인 기록 조회(필터 없는 버전)
    public Page<LoginHistoryResponseDTO> getAllLoginHistories(String username, int page, int size) {
        Pageable pageable = PageRequest.of(page, size,Sort.by(Sort.Direction.DESC, "loginAt"));
        return loginHistoryRepository
                .findByUsername(username, pageable)
                .map(LoginHistoryMapper::toDTO);
    }
    
    // 날짜 필터 조회
    public Page<LoginHistoryResponseDTO> getLoginHistories(String username, int page, int size, String date) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "loginAt"));
        Page<LoginHistory> result;

        // 날짜 필터
        if (date != null && !date.isEmpty()) {
            LocalDate targetDate = LocalDate.parse(date);
            LocalDateTime start = targetDate.atStartOfDay();
            LocalDateTime end = targetDate.atTime(23, 59, 59);

            result = loginHistoryRepository.findByUsernameAndLoginAtBetween(username, start, end, pageable);
        } else {
            result = loginHistoryRepository.findByUsername(username, pageable);
        }

        return result.map(LoginHistoryMapper::toDTO);
    }

    
    
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
    public void logout(LoginHistoryResponse loginHistory) {
        LoginHistory history = loginHistoryRepository
                .findById(loginHistory.id())
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