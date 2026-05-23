package com.example.authapp.domain.jwt.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.authapp.domain.audit.dto.LoginHistoryResponse;
import com.example.authapp.domain.audit.entity.LoginHistoryEntity;
import com.example.authapp.domain.jwt.entity.RefreshTokenEntity;
import com.example.authapp.domain.jwt.exception.JwtException;
import com.example.authapp.domain.jwt.repository.RefreshTokenRepository;
import com.example.authapp.util.JWTUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
	
    
    // ========================================================================================================
    // 조회
    // ========================================================================================================
    
    // JWT Refresh 존재 확인 메소드
    @Transactional(readOnly = true)
    public boolean existsRefresh(String refreshToken) {
        return refreshTokenRepository.existsByRefresh(refreshToken);
    }
    
    @Transactional(readOnly = true)
    public RefreshTokenEntity findByRefresh(String refreshToken) {
        return refreshTokenRepository.findByRefresh(refreshToken).orElseThrow(JwtException::tokenNotFound);
    }
    
    @Transactional(readOnly = true)
    public RefreshTokenEntity findByIdAndUsername(Long id, String username) {
        return refreshTokenRepository.findByIdAndUsername(id, username).orElseThrow(() -> new RuntimeException("세션 없음"));
    }
    
    @Transactional(readOnly = true)
    public List<RefreshTokenEntity> findByUsername(String username) {
        return refreshTokenRepository.findByUsername(username);
    }

    @Transactional(readOnly = true)
    public List<RefreshTokenEntity> findActiveByUsername(String username) {
        return refreshTokenRepository.findActiveSessionsByUsername(username);
    }
    
    // ========================================================================================================
    // 저장
    // ========================================================================================================

    public void save(RefreshTokenEntity entity) {
    	refreshTokenRepository.save(entity);
    }
    
    // JWT Refresh 토큰 발급 후 저장 메소드
    public void addRefresh(String username,String refreshToken,String ip,String userAgent,String device, LoginHistoryEntity loginHistory) {
        String jti = JWTUtil.getJti(refreshToken); 
    	RefreshTokenEntity entity = RefreshTokenEntity.builder()
                .username(username)
                .refresh(refreshToken)
                .jti(jti)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .ipAddress(ip)
                .userAgent(userAgent)
                .device(device)
                .revoked(false)
                .loginHistory(loginHistory) 
                .build();
    	refreshTokenRepository.save(entity);
    }
    
    // ========================================================================================================
    // 삭제
    // ========================================================================================================

    // JWT Refresh 토큰 삭제 메소드
    public void removeRefresh(String refreshToken) {
    	refreshTokenRepository.deleteByRefresh(refreshToken);
    }

    // 특정 유저 Refresh 토큰 모두 삭제 (탈퇴)
    public void removeRefreshUser(String username) {
    	refreshTokenRepository.deleteByUsername(username);
    }
    
    // ========================================================================================================
    // 상태 변경
    // ========================================================================================================

    // 리프레시 토큰 만료 : revoked -> true 처리
    @Transactional
    public LoginHistoryResponse revokeRefresh(String refreshToken) {
        RefreshTokenEntity entity = refreshTokenRepository
                .findByRefresh(refreshToken)
                .orElseThrow(JwtException::tokenNotFound);

        entity.revoke();
        
        LoginHistoryEntity history = entity.getLoginHistory();

        if (history == null) return null;

        return history.toResponse();  
    }
    
    // 전체 세션 로그아웃 -> 모든 리프레시토큰 만료(비밀번호 변경, 토큰 탈취 및 위험 감지)
    public void revokeAllByUsername(String username) {
        List<RefreshTokenEntity> tokens = refreshTokenRepository.findByUsername(username);

        for (RefreshTokenEntity token : tokens) {
            token.revoke();
        }
    }

    public void flush() {
    	refreshTokenRepository.flush();
    }
    
    
}
