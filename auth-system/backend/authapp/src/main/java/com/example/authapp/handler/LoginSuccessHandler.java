package com.example.authapp.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.example.authapp.domain.jwt.service.JwtService;
import com.example.authapp.domain.user.entity.UserEntity;
import com.example.authapp.handler.dto.LoginResponseDTO;
import com.example.authapp.handler.dto.UserResponseDTO;
import com.example.authapp.principal.UserPrincipal;
import com.example.authapp.util.CookieService;
import com.example.authapp.util.JWTUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

@Component
@Qualifier("LoginSuccessHandler")
@RequiredArgsConstructor
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtService jwtService;
	private final CookieService cookieService;	
	private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

    	// 실제 유저 꺼내기
    	UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
    	UserEntity user = principal.getUser();
    	
        // username, role
        String username =  authentication.getName();
        String role = authentication.getAuthorities().iterator().next().getAuthority();

        // JWT(Access/Refresh) 발급
        String accessToken = JWTUtil.createJWT(username, role, true);
        String refreshToken = JWTUtil.createJWT(username, role, false);
        long expiresIn = JWTUtil.getAccessTokenExpiresIn();

        // 클라이언트 정보 추출
        String ip = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");
        String device = parseDevice(userAgent);
        
        // 발급한 Refresh DB 테이블 저장 (Refresh whitelist)
        //jwtService.addRefresh(username, refreshToken);
        jwtService.addRefresh(username, refreshToken, ip, userAgent, device);
        
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

//        String json = String.format("{\"accessToken\":\"%s\", \"refreshToken\":\"%s\"}", accessToken, refreshToken);
//        response.getWriter().write(json);
//        response.getWriter().flush();
    }
    
    // device 파싱 (간단 버전)
    private String parseDevice(String userAgent) {
        if (userAgent == null) return "Unknown";
        if (userAgent.contains("Mobile")) return "Mobile";
        if (userAgent.contains("Windows")) return "Windows";
        if (userAgent.contains("Mac")) return "Mac";
        if (userAgent.contains("Linux")) return "Linux";
        return "Other";
    }
    

}