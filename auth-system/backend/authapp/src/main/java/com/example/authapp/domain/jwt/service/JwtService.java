package com.example.authapp.domain.jwt.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.authapp.domain.audit.dto.LoginHistoryResponse;
import com.example.authapp.domain.audit.entity.LoginHistoryEntity;
import com.example.authapp.domain.audit.entity.AuthEventType;
import com.example.authapp.domain.audit.service.AuthEventLogService;
import com.example.authapp.domain.jwt.dto.JWTResponseDTO;
import com.example.authapp.domain.jwt.entity.RefreshTokenEntity;
import com.example.authapp.domain.jwt.exception.JwtException;
import com.example.authapp.domain.jwt.repository.RefreshTokenRepository;
import com.example.authapp.domain.risk.exception.RiskException;
import com.example.authapp.domain.risk.service.RiskService;
import com.example.authapp.util.ClientUtil;
import com.example.authapp.util.JWTUtil;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JwtService {

	private final RefreshTokenService refreshTokenService;
    private final CookieService cookieService;
    
    private final AuthEventLogService securityEventService;
    private final RiskService riskService;
    
    
    // 소셜 로그인 성공 후 쿠키(Refresh) -> 헤더 방식으로 응답
    @Transactional
    public JWTResponseDTO cookie2Header(HttpServletRequest request,HttpServletResponse response){

    	// 쿠키에서 토큰 추출
    	String refreshToken = extractRefreshToken(request);

        // Refresh 토큰 검증 -> false 면 refresh 토큰 검증
        try {
        	JWTUtil.validate(refreshToken, false);
        } catch (ExpiredJwtException e) {
            throw new RuntimeException("refreshToken 만료됨");
        } catch (JwtException e) {
            throw  JwtException.invalidRefreshToken();
        }

        // 정보 추출
        String username = JWTUtil.getUsername(refreshToken);
        String role = JWTUtil.getRole(refreshToken);

        // 토큰 생성
        String jti = UUID.randomUUID().toString();
        String newAccessToken = JWTUtil.createJWT(username, role, jti, true);
        String newRefreshToken = JWTUtil.createJWT(username, role, jti, false);
        
        // JWT에서 jti 추출
        String newJti = JWTUtil.getJti(newRefreshToken);

        // 기존 Refresh 토큰 DB 삭제 후 신규 추가
        RefreshTokenEntity newRefreshEntity = RefreshTokenEntity.builder()
                .username(username)
                .refresh(newRefreshToken)
                .jti(newJti)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .build();

        refreshTokenService.removeRefresh(refreshToken);
        refreshTokenService.flush(); // 같은 트랜잭션 내부라 : 삭제 -> 생성 문제 해결
        refreshTokenService.save(newRefreshEntity);

        // 기존 쿠키 제거
        Cookie refreshCookie = new Cookie("refreshToken", null);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(false);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(10);
        response.addCookie(refreshCookie);

        return new JWTResponseDTO(newAccessToken, newRefreshToken);
    }

    // Refresh 토큰으로 Access 토큰 재발급 로직 (Rotate 포함)
    @Transactional
    public JWTResponseDTO refreshRotate(HttpServletRequest request, HttpServletResponse response) {

    	// 쿠키에서 토큰 추출
    	String refreshToken = extractRefreshToken(request);

    	// 요청으로 보낸 토큰이 DB에 있는지 조회
        RefreshTokenEntity oldEntity = refreshTokenService.findByRefresh(refreshToken);
        
        String username = oldEntity.getUsername();
        
        // 클라이언트 정보
        String ip = ClientUtil.getIp(request);
        String userAgent = ClientUtil.getUserAgent(request);
        String device = ClientUtil.getDevice(userAgent);

        // 탈취 판단 및 차단 위임**********************************************
        boolean available = riskService.analyzeTokenRisk(oldEntity, ip, device, userAgent);  
        if (!available) {
            //throw RiskException.tokenReuseDetected();
        	response.setStatus(401);
            return new JWTResponseDTO(null, null);
        }
        
        // 이 아래는 정상 흐름************************************************
        
        // 토큰 생성
        String jti = UUID.randomUUID().toString();
        String role = JWTUtil.getRole(refreshToken);
        String newAccessToken = JWTUtil.createJWT(username, role, jti, true);
        String newRefreshToken = JWTUtil.createJWT(username, role, jti, false);
        
        // 기존 토큰 revoke
        oldEntity.revoke();
        // 기존 토큰 로테이션 처리 
        oldEntity.setReplacedByToken(newRefreshToken);
        
        // 기존 Refresh 토큰 DB 삭제 후 신규 추가
        RefreshTokenEntity newEntity = RefreshTokenEntity.builder()
                .username(username)
                .refresh(newRefreshToken)
                .jti(jti)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .ipAddress(oldEntity.getIpAddress())
                .userAgent(oldEntity.getUserAgent())
                .device(oldEntity.getDevice())
                .revoked(false)
                .loginHistory(oldEntity.getLoginHistory())
                .build();

        refreshTokenService.save(newEntity);

        // 쿠키 교체
        cookieService.addRefreshCookie(response, newRefreshToken);

        // 이벤트 기록
        securityEventService.tokenReissue(username);
        
        // 리프레시 토큰 바디로 안넘겨줌
        return new JWTResponseDTO(newAccessToken, null);
        
    }
    
    // 리프레시 토큰 추출
    private String extractRefreshToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
        	throw JwtException.refreshCookieNotFound();
        }
        for (Cookie cookie : cookies) {
            if ("refreshToken".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        throw JwtException.refreshTokenNotFound();
    }

    
    

}
