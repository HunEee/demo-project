package com.example.authapp.domain.admin;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.authapp.domain.audit.entity.LoginHistoryEntity;
import com.example.authapp.domain.audit.entity.AuthEventLogEntity;
import com.example.authapp.domain.audit.repository.LoginHistoryRepository;
import com.example.authapp.domain.audit.repository.AuthEventLogRepository;
import com.example.authapp.domain.jwt.entity.RefreshTokenEntity;
import com.example.authapp.domain.jwt.repository.RefreshRepository;
import com.example.authapp.domain.user.entity.UserEntity;
import com.example.authapp.domain.user.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminService {
	
    private final UserRepository userRepository;
    private final LoginHistoryRepository loginHistoryRepository;
    private final AuthEventLogRepository securityEventRepository;
    private final RefreshRepository refreshTokenRepository;

    // 전체 유저 조회
    public List<UserEntity> getAllUsers() {
        return userRepository.findAll();
    }

    // 관리자 목록
    public List<UserEntity> getAdmins() {
        return userRepository.findAllAdmins();
    }

    // 계정 잠금
    public void lockUser(Long userId) {
        UserEntity user = getUser(userId);
        user.lock();
    }

    // 계정 잠금 해제
    public void unlockUser(Long userId) {
        UserEntity user = getUser(userId);
        user.unlock();
    }

    // 로그인 기록 조회
    public List<LoginHistoryEntity> getLoginHistory(String username) {
        return loginHistoryRepository.findByUsername(username);
    }

    // 보안 이벤트 조회
    public List<AuthEventLogEntity> getSecurityEvents(String username) {
        return securityEventRepository.findByUsername(username);
    }

    // 토큰 강제 만료
    public void revokeAllTokens(String username) {
        List<RefreshTokenEntity> tokens = refreshTokenRepository.findByUsername(username);
        for (RefreshTokenEntity token : tokens) {
            token.revoke();
        }
    }

    private UserEntity getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
