package com.example.authapp.security.handler;

import java.io.IOException;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.example.authapp.domain.audit.service.LoginHistoryService;
import com.example.authapp.domain.audit.service.AuthEventLogService;
import com.example.authapp.domain.jwt.service.CookieService;
import com.example.authapp.domain.jwt.service.JwtService;
import com.example.authapp.domain.jwt.service.RefreshTokenService;
import com.example.authapp.domain.risk.service.RiskService;
import com.example.authapp.domain.user.entity.UserEntity;
import com.example.authapp.security.handler.dto.LoginResponseDTO;
import com.example.authapp.security.handler.dto.UserResponseDTO;
import com.example.authapp.security.principal.UserPrincipal;
import com.example.authapp.util.ClientUtil;
import com.example.authapp.util.JWTUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component("loginSuccessHandler")
@RequiredArgsConstructor
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final RefreshTokenService refreshTokenService;
	private final CookieService cookieService;	
	private final ObjectMapper objectMapper;
	private final LoginHistoryService loginHistoryService;
	private final AuthEventLogService securityEventService;
	private final RiskService riskService;
    
	@Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

    	// 실제 유저 꺼내기
    	UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
    	UserEntity user = principal.getUser();
    	
        // username, role
        String username =  authentication.getName();
        String role = authentication.getAuthorities().iterator().next().getAuthority();

        // JWT(Access/Refresh) 발급
        String jti = UUID.randomUUID().toString();
        String accessToken = JWTUtil.createJWT(username, role, jti, true);
        String refreshToken = JWTUtil.createJWT(username, role, jti, false);
        long expiresIn = JWTUtil.getAccessTokenExpiresIn();

        // 클라이언트 정보 추출
        String ip = ClientUtil.getIp(request);
        String userAgent = ClientUtil.getUserAgent(request);
        String device = ClientUtil.getDevice(userAgent);
        
        // 로그인 이력 저장
        var history = loginHistoryService.saveSuccess(username,ip,userAgent,device);
        
        // 위험 로그인 여부 판단
        riskService.analyzeLoginRisk(history);
        
        // 발급한 Refresh DB 테이블 저장 (Refresh whitelist)
        //jwtService.addRefresh(username, refreshToken);
        refreshTokenService.addRefresh(username, refreshToken, ip, userAgent, device, history);

        // 쿠키 저장
        cookieService.addRefreshCookie(response, refreshToken);
        
        UserResponseDTO userDTO = UserResponseDTO.from(user);
        LoginResponseDTO result = LoginResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(expiresIn)
                .user(userDTO)
                .build();
        
        // 응답
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), result);

        //로그인 성공/실패 기록
        securityEventService.loginSuccess(username);
        
    }
    


}