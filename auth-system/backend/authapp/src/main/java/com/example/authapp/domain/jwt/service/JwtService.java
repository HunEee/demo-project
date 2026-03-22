package com.example.authapp.domain.jwt.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.authapp.domain.jwt.dto.JWTResponseDTO;
import com.example.authapp.domain.jwt.dto.RefreshRequestDTO;
import com.example.authapp.domain.jwt.entity.RefreshEntity;
import com.example.authapp.domain.jwt.repository.RefreshRepository;
import com.example.authapp.util.CookieService;
import com.example.authapp.util.JWTUtil;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final RefreshRepository refreshRepository;
    private final CookieService cookieService;

    // 소셜 로그인 성공 후 쿠키(Refresh) -> 헤더 방식으로 응답
    @Transactional
    public JWTResponseDTO cookie2Header(HttpServletRequest request,HttpServletResponse response){

        // 쿠키 리스트
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            throw new RuntimeException("쿠키가 존재하지 않습니다.");
        }

        // Refresh 토큰 획득
        String refreshToken = null;
        for (Cookie cookie : cookies) {
            if ("refreshToken".equals(cookie.getName())) {
                refreshToken = cookie.getValue();
                break;
            }
        }

        if (refreshToken == null) {
            throw new RuntimeException("refreshToken 쿠키가 없습니다.");
        }

        // Refresh 토큰 검증 -> false 면 refresh 토큰 검증
        try {
        	JWTUtil.validate(refreshToken, false);
        } catch (ExpiredJwtException e) {
            throw new RuntimeException("refreshToken 만료됨");
        } catch (JwtException e) {
            throw new RuntimeException("유효하지 않은 refreshToken");
        }

        // 정보 추출
        String username = JWTUtil.getUsername(refreshToken);
        String role = JWTUtil.getRole(refreshToken);

        // 토큰 생성
        String newAccessToken = JWTUtil.createJWT(username, role, true);
        String newRefreshToken = JWTUtil.createJWT(username, role, false);
        
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

//        String refreshToken = dto.getRefreshToken();

//        // Refresh 토큰 검증
//        Boolean isValid = JWTUtil.isValid(refreshToken, false);
//        if (!isValid) {
//            throw new RuntimeException("유효하지 않은 refreshToken입니다.");
//        }
//
//        // RefreshEntity 존재 확인 (화이트리스트)
//        if (!existsRefresh(refreshToken)) {
//            throw new RuntimeException("유효하지 않은 refreshToken입니다.");
//        }
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            throw new RuntimeException("refresh cookie 없음");
        }

        String refreshToken = null;
        
        for (Cookie cookie : cookies) {
            if ("refreshToken".equals(cookie.getName())) {
                refreshToken = cookie.getValue();
            }
        }

        if (refreshToken == null) {
            throw new RuntimeException("refresh cookie 없음");
        }

        RefreshEntity oldEntity = refreshRepository.findByRefresh(refreshToken)
                					.orElseThrow(() -> new RuntimeException("유효하지 않은 refreshToken입니다."));
        
        if (oldEntity.isRevoked()) {
            throw new RuntimeException("만료된 refreshToken입니다.");
        }
     
        // 정보 추출
        String username = JWTUtil.getUsername(refreshToken);
        String role = JWTUtil.getRole(refreshToken);

        // 토큰 생성
        String newAccessToken = JWTUtil.createJWT(username, role, true);
        String newRefreshToken = JWTUtil.createJWT(username, role, false);

        String newJti = JWTUtil.getJti(newRefreshToken);
        
        // 기존 토큰 revoke
        oldEntity.revoke();
        // 기존 토큰 로테이션 처리 
        oldEntity.setReplacedByToken(newRefreshToken);
        
        // 기존 Refresh 토큰 DB 삭제 후 신규 추가
        RefreshEntity newEntity = RefreshEntity.builder()
                .username(username)
                .refresh(newRefreshToken)
                .jti(newJti)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .ipAddress(oldEntity.getIpAddress())
                .userAgent(oldEntity.getUserAgent())
                .device(oldEntity.getDevice())
                .revoked(false)
                .build();

        refreshRepository.save(newEntity);

        // 쿠키 교체
        cookieService.addRefreshCookie(response, newRefreshToken);
        
        // 리프레시 토큰 바디로 안넘겨줌
        return new JWTResponseDTO(newAccessToken, null);
    }
    
    
    // JWT Refresh 토큰 발급 후 저장 메소드
    @Transactional
    public void addRefresh(String username,String refreshToken,String ip,String userAgent,String device) {
        RefreshEntity entity = RefreshEntity.builder()
                .username(username)
                .refresh(refreshToken)
                .jti(UUID.randomUUID().toString())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .ipAddress(ip)
                .userAgent(userAgent)
                .device(device)
                .revoked(false)
                .build();
        refreshRepository.save(entity);
    }
    
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
    public void revokeRefresh(String refreshToken) {
        RefreshEntity entity = refreshRepository
                .findByRefresh(refreshToken)
                .orElseThrow(() -> new RuntimeException("Refresh token not found"));

        entity.revoke();
    
    }
    

}
