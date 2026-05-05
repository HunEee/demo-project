package com.example.authapp.security.handler;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;

import com.example.authapp.domain.audit.dto.LoginHistoryResponse;
import com.example.authapp.domain.audit.service.LoginHistoryService;
import com.example.authapp.domain.audit.service.AuthEventLogService;
import com.example.authapp.domain.jwt.service.CookieService;
import com.example.authapp.domain.jwt.service.JwtService;
import com.example.authapp.domain.jwt.service.RefreshTokenService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RefreshTokenLogoutHandler implements LogoutHandler {

    private final RefreshTokenService refreshTokenService;
    private final CookieService cookieService;
    private final LoginHistoryService loginHistoryService;
    private final AuthEventLogService securityEventService;

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        
    	Cookie[] cookies = request.getCookies();
        if (cookies == null) return;

        for (Cookie cookie : cookies) {
            if ("refreshToken".equals(cookie.getName())) {
                String refreshToken = cookie.getValue();
                //revoke + historyId 가져오기
                LoginHistoryResponse history = refreshTokenService.revokeRefresh(refreshToken);
                // 로그인 이력 업데이트
                if (history != null) {
                    loginHistoryService.logout(history);
                    securityEventService.logout(history.username());
                }
                // 쿠키 삭제
                cookieService.clearRefreshCookie(response);
            }
        }
        
    }

    
}
