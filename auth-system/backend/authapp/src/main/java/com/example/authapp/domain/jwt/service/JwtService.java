package com.example.authapp.domain.jwt.service;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.authapp.domain.audit.service.AuthEventLogService;
import com.example.authapp.domain.jwt.dto.JWTResponseDTO;
import com.example.authapp.domain.jwt.entity.RefreshTokenEntity;
import com.example.authapp.domain.jwt.exception.JwtException;
import com.example.authapp.domain.risk.service.RiskService;
import com.example.authapp.domain.user.service.UserQueryService;
import com.example.authapp.security.handler.dto.UserResponseDTO;
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
    private final UserQueryService userQueryService;
    
    private final AuthEventLogService securityEventService;
    private final RiskService riskService;
    
    
    // 소셜 로그인 성공 후 쿠키(Refresh) -> 헤더 방식으로 응답
    @Transactional
    public JWTResponseDTO cookie2Header(HttpServletRequest request,HttpServletResponse response){
    	String refreshToken = extractRefreshToken(request);
        validateRefreshToken(refreshToken);

        RefreshTokenEntity oldEntity = refreshTokenService.findByRefresh(refreshToken);
        String ip = ClientUtil.getIp(request);
        String userAgent = ClientUtil.getUserAgent(request);
        String device = ClientUtil.getDevice(userAgent);

        boolean available = riskService.analyzeTokenRisk(oldEntity, ip, device, userAgent);
        if (!available) {
            throw JwtException.revokedRefreshToken();
        }

        return rotateRefreshToken(refreshToken, oldEntity, response, ip, userAgent, device);
    }

    // Refresh 토큰으로 Access 토큰 재발급 로직 (Rotate 포함)
    @Transactional
    public JWTResponseDTO refreshRotate(HttpServletRequest request, HttpServletResponse response) {
    	String refreshToken = extractRefreshToken(request);
        RefreshTokenEntity oldEntity = refreshTokenService.findByRefresh(refreshToken);
        
        String ip = ClientUtil.getIp(request);
        String userAgent = ClientUtil.getUserAgent(request);
        String device = ClientUtil.getDevice(userAgent);

        boolean available = riskService.analyzeTokenRisk(oldEntity, ip, device, userAgent);  
        if (!available) {
        	response.setStatus(401);
            return new JWTResponseDTO(null);
        }

        JWTResponseDTO result = rotateRefreshToken(refreshToken, oldEntity, response, ip, userAgent, device);
        securityEventService.tokenReissue(oldEntity.getUsername());
        return result;
    }
    
    private JWTResponseDTO rotateRefreshToken(
            String refreshToken,
            RefreshTokenEntity oldEntity,
            HttpServletResponse response,
            String ip,
            String userAgent,
            String device
    ) {
        String username = oldEntity.getUsername();
        Set<String> roles = JWTUtil.getRoles(refreshToken);

        String jti = UUID.randomUUID().toString();
        String newAccessToken = JWTUtil.createJWT(username, roles, jti, true);
        String newRefreshToken = JWTUtil.createJWT(username, roles, jti, false);

        oldEntity.revoke();
        oldEntity.setReplacedByToken(newRefreshToken);

        RefreshTokenEntity newEntity = RefreshTokenEntity.builder()
                .username(username)
                .refresh(newRefreshToken)
                .jti(jti)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .ipAddress(ip)
                .userAgent(userAgent)
                .device(device)
                .revoked(false)
                .loginHistory(oldEntity.getLoginHistory())
                .build();

        refreshTokenService.save(newEntity);
        cookieService.addRefreshCookie(response, newRefreshToken);

        UserResponseDTO user = UserResponseDTO.from(userQueryService.getByUsername(username), roles);
        return new JWTResponseDTO(newAccessToken, user, JWTUtil.getAccessTokenExpiresIn());
    }

    private void validateRefreshToken(String refreshToken) {
        try {
        	JWTUtil.validate(refreshToken, false);
        } catch (ExpiredJwtException e) {
            throw JwtException.expiredRefreshToken();
        } catch (io.jsonwebtoken.JwtException e) {
            throw JwtException.invalidRefreshToken();
        }
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
