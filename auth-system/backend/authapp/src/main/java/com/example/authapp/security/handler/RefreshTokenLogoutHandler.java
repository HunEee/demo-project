package com.example.authapp.security.handler;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;

import com.example.authapp.domain.audit.dto.LoginHistoryResponse;
import com.example.authapp.domain.audit.service.LoginHistoryService;
import com.example.authapp.domain.audit.service.AuthEventLogService;
import com.example.authapp.application.auth.usecase.CookieService;
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
                // refresh token을 폐기하고 로그인 이력 정보를 가져온다.
                LoginHistoryResponse history = refreshTokenService.revokeRefresh(refreshToken);
                // 로그아웃 이력과 보안 이벤트를 기록한다.
                if (history != null) {
                    loginHistoryService.logout(history);
                    securityEventService.logout(history.username());
                }
                // refresh cookie를 제거한다.
                cookieService.clearRefreshCookie(response);
            }
        }
        
    }

    
}
