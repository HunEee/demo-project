package com.example.authapp.domain.jwt.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.authapp.domain.audit.dto.LoginHistoryResponse;
import com.example.authapp.domain.audit.entity.LoginHistoryEntity;
import com.example.authapp.domain.jwt.entity.RefreshTokenEntity;
import com.example.authapp.domain.jwt.exception.JwtException;
import com.example.authapp.domain.jwt.repository.RefreshTokenRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenSettingsService tokenSettingsService;
	
    
    // ========================================================================================================
    // 조회
    // ========================================================================================================
    
    // JWT Refresh 존재 확인 메소드
    @Transactional(readOnly = true)
    public boolean existsRefresh(String refreshToken) {
        return refreshTokenRepository.existsByRefreshTokenHash(hashToken(refreshToken));
    }
    
    @Transactional(readOnly = true)
    public RefreshTokenEntity findByRefresh(String refreshToken) {
        return refreshTokenRepository.findByRefreshTokenHash(hashToken(refreshToken)).orElseThrow(JwtException::tokenNotFound);
    }

    @Transactional
    public RefreshTokenEntity findByRefreshForUpdate(String refreshToken) {
        return refreshTokenRepository.findByRefreshTokenHashForUpdate(hashToken(refreshToken))
                .orElseThrow(JwtException::tokenNotFound);
    }

    @Transactional(readOnly = true)
    public boolean existsActiveSession(String username, String jti) {
        return refreshTokenRepository.existsByUsernameAndJtiAndRevokedFalse(username, jti);
    }

    @Transactional(readOnly = true)
    public Optional<RefreshTokenEntity> findActiveByUsernameAndJti(String username, String jti) {
        return refreshTokenRepository.findByUsernameAndJtiAndRevokedFalse(username, jti);
    }

    @Transactional(readOnly = true)
    public Optional<RefreshTokenEntity> findActiveReplacement(String familyId, String jti) {
        return refreshTokenRepository.findByFamilyIdAndJtiAndRevokedFalse(familyId, jti);
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
        String jti = jwtTokenProvider.getJti(refreshToken); 
    	RefreshTokenEntity entity = RefreshTokenEntity.builder()
                .username(username)
                .refreshTokenHash(hashToken(refreshToken))
                .jti(jti)
                .familyId(jti)
                .tokenSequence(0L)
                .expiresAt(LocalDateTime.now().plusDays(tokenSettingsService.current().getRefreshTokenLifetimeDays()))
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
        refreshTokenRepository.findByRefreshTokenHash(hashToken(refreshToken))
                .ifPresent(refreshTokenRepository::delete);
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
        return revokeRefresh(refreshToken, "LOGOUT", null);
    }

    @Transactional
    public LoginHistoryResponse revokeRefresh(String refreshToken, String reason, String actorUsername) {
        RefreshTokenEntity entity = refreshTokenRepository
                .findByRefreshTokenHash(hashToken(refreshToken))
                .orElseThrow(JwtException::tokenNotFound);

        entity.revokeBy(reason, defaultActor(actorUsername, entity.getUsername()));
        
        LoginHistoryEntity history = entity.getLoginHistory();

        if (history == null) return null;

        return history.toResponse();  
    }
    
    // 전체 세션 로그아웃 -> 모든 리프레시토큰 만료(비밀번호 변경, 토큰 탈취 및 위험 감지)
    public void revokeAllByUsername(String username) {
        revokeAllByUsername(username, "USER_REVOKE", username);
    }

    public void revokeAllByUsername(String username, String reason) {
        revokeAllByUsername(username, reason, username);
    }

    public void revokeAllByUsername(String username, String reason, String actorUsername) {
        String actor = defaultActor(actorUsername, username);
        refreshTokenRepository.revokeActiveByUsername(username, reason, actor, LocalDateTime.now());
    }

    public int revokeActiveByUsername(String username, String reason, String actorUsername) {
        String actor = defaultActor(actorUsername, username);
        return refreshTokenRepository.revokeActiveByUsername(username, reason, actor, LocalDateTime.now());
    }

    public static String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private String defaultActor(String actorUsername, String fallbackUsername) {
        if (actorUsername == null || actorUsername.isBlank()) {
            return fallbackUsername;
        }
        return actorUsername;
    }

    public void flush() {
    	refreshTokenRepository.flush();
    }
    
    
}
