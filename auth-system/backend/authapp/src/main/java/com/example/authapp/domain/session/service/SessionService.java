package com.example.authapp.domain.session.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.authapp.domain.audit.dto.LoginHistoryResponse;
import com.example.authapp.domain.audit.entity.LoginHistoryEntity;
import com.example.authapp.domain.audit.service.LoginHistoryService;
import com.example.authapp.domain.audit.service.AuthEventLogService;
import com.example.authapp.domain.jwt.entity.RefreshEntity;
import com.example.authapp.domain.jwt.repository.RefreshRepository;
import com.example.authapp.domain.jwt.service.JwtService;
import com.example.authapp.domain.session.dto.SessionResponse;
import com.example.authapp.util.JWTUtil;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SessionService {
	
	private final JwtService jwtService;
    private final RefreshRepository refreshRepository;
    private final LoginHistoryService loginHistoryService;
    private final AuthEventLogService securityEventService;
    
    // 세션 목록 조회
    public List<SessionResponse> getSessions(String username, HttpServletRequest request) {
    	// 현재 쿠키에서 리프레시 토큰을 꺼내고 현재 세션 세팅 
        String refreshToken = extractRefreshToken(request);
        String currentJti = JWTUtil.getJti(refreshToken);
    	
        List<RefreshEntity> tokens = refreshRepository.findByUsernameAndRevokedFalse(username);

        return tokens.stream()
                .map(token -> SessionResponse.builder()
                        .id(token.getId())
                        .ip(token.getIpAddress())
                        .device(token.getDevice())
                        .createdAt(loginHistoryService.getSessionStartTime(token.getLoginHistory().getId()))
                        .lastAccessAt(token.getLastUsedAt() != null
                                ? token.getLastUsedAt().toString()
                                : token.getCreatedDate().toString())
                        .current(token.getJti().equals(currentJti))
                        .build()
                ).toList();
    }

    // 특정 세션 로그아웃
    @Transactional
    public void logoutSession(Long id, String username) {
        RefreshEntity refreshToken = refreshRepository
                .findByIdAndUsername(id, username)
                .orElseThrow(() -> new RuntimeException("세션 없음"));

        // 리프레시 토큰 만료
        refreshToken.revoke();
        
        // 로그 기록과 이벤트 기록을 남김
        processLogout(refreshToken.getRefresh());
        
    }

    // 전체 로그아웃
    @Transactional
    public void logoutAll(String username, HttpServletRequest request) {
    	// 현재 쿠키에서 리프레시 토큰을 꺼내고 현재 세션 세팅 
    	String refreshToken = extractRefreshToken(request);
        String currentJti = JWTUtil.getJti(refreshToken);
    	
    	List<RefreshEntity> tokens = refreshRepository.findByUsernameAndRevokedFalse(username);

        tokens.stream()
                .filter(t -> !t.getJti().equals(currentJti)) // 현재 세션 제외
                .forEach(t -> {
                    t.revoke();
                    processLogout(t.getRefresh());
                });
        
    }
    
    //*******************************************************************************************
    // 내부 메서드
    //*******************************************************************************************
    
    // 쿠키 추출
    private String extractRefreshToken(HttpServletRequest request) {
        if (request.getCookies() == null) {
            throw new RuntimeException("쿠키 없음");
        }
        for (Cookie cookie : request.getCookies()) {
            if ("refreshToken".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        throw new RuntimeException("refreshToken 없음");
    }
    
    // 로그아웃 기록 및 이벤트 기록
    private void processLogout(String refreshTokenValue) {
        LoginHistoryResponse history = jwtService.revokeRefresh(refreshTokenValue);

        if (history != null) {
            loginHistoryService.logout(history);
            securityEventService.logout(history.username(), history.ipAddress(), history.device());
        }
    }
    
}
