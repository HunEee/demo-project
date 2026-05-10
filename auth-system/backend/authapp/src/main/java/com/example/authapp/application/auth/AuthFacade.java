package com.example.authapp.application.auth;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.authapp.application.auth.dto.LoginResponseDTO;
import com.example.authapp.domain.audit.service.AuthEventLogService;
import com.example.authapp.domain.audit.service.LoginHistoryService;
import com.example.authapp.domain.jwt.service.CookieService;
import com.example.authapp.domain.jwt.service.RefreshTokenService;
import com.example.authapp.domain.risk.service.RiskService;
import com.example.authapp.domain.user.entity.UserEntity;
import com.example.authapp.domain.user.service.UserQueryService;
import com.example.authapp.security.handler.dto.UserResponseDTO;
import com.example.authapp.security.principal.UserPrincipal;
import com.example.authapp.util.ClientUtil;
import com.example.authapp.util.JWTUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthFacade {

    private final UserQueryService userQueryService;
    private final RefreshTokenService refreshTokenService;
    private final CookieService cookieService;
    private final LoginHistoryService loginHistoryService;
    private final RiskService riskService;
    private final AuthEventLogService authEventLogService;

    
    public LoginResponseDTO loginSuccess(UserPrincipal principal, HttpServletRequest request, HttpServletResponse response) {

        UserEntity user = userQueryService.getUser(principal.getUserId());

        String username = principal.getUsername();

        Set<String> roles = principal.getAuthorities()
                        .stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toSet());

        // JWT(Access/Refresh) 발급
        String jti = UUID.randomUUID().toString();
        String accessToken = JWTUtil.createJWT(username, roles, jti, true);
        String refreshToken = JWTUtil.createJWT(username, roles, jti, false);
        long expiresIn = JWTUtil.getAccessTokenExpiresIn();

        
        String ip = ClientUtil.getIp(request);
        String userAgent = ClientUtil.getUserAgent(request);
        String device = ClientUtil.getDevice(userAgent);

        var history = loginHistoryService.saveSuccess(username, ip, userAgent, device);

        riskService.analyzeLoginRisk(user,history);

        refreshTokenService.addRefresh(username, refreshToken, ip, userAgent, device, history);

        cookieService.addRefreshCookie(response,refreshToken);

        authEventLogService.loginSuccess(username);

        return LoginResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(expiresIn)
                .user(UserResponseDTO.from(user))
                .build();
    }
    
    
    
    public void socialLoginSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {

        String username = authentication.getName();

        Set<String> roles = authentication.getAuthorities()
                        .stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toSet());

        // JWT(Refresh) 발급
        String jti = UUID.randomUUID().toString();
        String refreshToken = JWTUtil.createJWT(username, roles, jti, false);

        // 클라이언트 정보 추출     
        String ip = ClientUtil.getIp(request);
        String userAgent = ClientUtil.getUserAgent(request);
        String device = ClientUtil.getDevice(userAgent);


        var history = loginHistoryService.saveSuccess(username, ip, userAgent, device);

        refreshTokenService.addRefresh(username, refreshToken, ip, userAgent, device, history);

        authEventLogService.loginSuccess(username);

        cookieService.addRefreshCookie(response,refreshToken);

        response.sendRedirect("http://localhost:5173/cookie");
    }
    
    
    
    
}