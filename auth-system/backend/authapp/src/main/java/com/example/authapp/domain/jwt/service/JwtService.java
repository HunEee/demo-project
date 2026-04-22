package com.example.authapp.domain.jwt.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.authapp.domain.audit.dto.LoginHistoryResponse;
import com.example.authapp.domain.audit.entity.LoginHistory;
import com.example.authapp.domain.audit.entity.SecurityEventType;
import com.example.authapp.domain.audit.service.SecurityEventService;
import com.example.authapp.domain.jwt.dto.JWTResponseDTO;
import com.example.authapp.domain.jwt.entity.RefreshEntity;
import com.example.authapp.domain.jwt.exception.JwtException;
import com.example.authapp.domain.jwt.repository.RefreshRepository;
import com.example.authapp.domain.risk.service.RiskService;
import com.example.authapp.util.ClientUtil;
import com.example.authapp.util.CookieService;
import com.example.authapp.util.JWTUtil;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final RefreshRepository refreshRepository;
    private final CookieService cookieService;
    private final SecurityEventService securityEventService;
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
        RefreshEntity newRefreshEntity = RefreshEntity.builder()
                .username(username)
                .refresh(newRefreshToken)
                .jti(newJti)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .build();

        removeRefresh(refreshToken);
        refreshRepository.flush(); // 같은 트랜잭션 내부라 : 삭제 -> 생성 문제 해결
        refreshRepository.save(newRefreshEntity);

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
        RefreshEntity oldEntity = refreshRepository.findByRefresh(refreshToken).orElseThrow(JwtException::tokenNotFound);
        
        String username = oldEntity.getUsername();
        
        // 클라이언트 정보
        String ip = ClientUtil.getIp(request);
        String userAgent = ClientUtil.getUserAgent(request);
        String device = ClientUtil.getDevice(userAgent);

        // 탈취 판단 및 차단 위임**********************************************
        riskService.analyzeTokenRisk(oldEntity, ip, device, userAgent);  
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
        RefreshEntity newEntity = RefreshEntity.builder()
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

        refreshRepository.save(newEntity);

        // 쿠키 교체
        cookieService.addRefreshCookie(response, newRefreshToken);

        // 이벤트 기록
        securityEventService.tokenReissue(username, ip, device);
        
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
    
    
    // JWT Refresh 토큰 발급 후 저장 메소드
    @Transactional
    public void addRefresh(String username,String refreshToken,String ip,String userAgent,String device, LoginHistory loginHistory) {
        String jti = JWTUtil.getJti(refreshToken); 
    	RefreshEntity entity = RefreshEntity.builder()
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
        refreshRepository.save(entity);
    }
    
    //******************************************************************************************************************
    // JWT Refresh 토큰 관련 메서드
    //******************************************************************************************************************
    
    // JWT Refresh 존재 확인 메소드
    @Transactional(readOnly = true)
    public Boolean existsRefresh(String refreshToken) {
        return refreshRepository.existsByRefresh(refreshToken);
    }

    // JWT Refresh 토큰 삭제 메소드
    public void removeRefresh(String refreshToken) {
        refreshRepository.deleteByRefresh(refreshToken);
    }
    
    // 특정 유저 Refresh 토큰 모두 삭제 (탈퇴)
    public void removeRefreshUser(String username) {
        refreshRepository.deleteByUsername(username);
    }
    
    // 리프레시 토큰 만료 : revoked -> true 처리
    @Transactional
    public LoginHistoryResponse revokeRefresh(String refreshToken) {
        RefreshEntity entity = refreshRepository
                .findByRefresh(refreshToken)
                .orElseThrow(JwtException::tokenNotFound);

        entity.revoke();
        
        LoginHistory history = entity.getLoginHistory();

        if (history == null) return null;

        return history.toResponse();  
    }
    
    // 전체 세션 로그아웃 -> 모든 리프레시토큰 만료(비밀번호 변경)
    @Transactional
    public void revokeAllByUsername(String username) {
        List<RefreshEntity> tokens = refreshRepository.findByUsername(username);

        for (RefreshEntity token : tokens) {
            token.revoke();
        }
    }
    
    

}
