package com.example.authapp.security.handler;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;

import com.example.authapp.domain.audit.service.LoginHistoryService;
import com.example.authapp.domain.jwt.service.JwtService;
import com.example.authapp.util.CookieService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RefreshTokenLogoutHandler implements LogoutHandler {

    private final JwtService jwtService;
    private final CookieService cookieService;
    private final LoginHistoryService loginHistoryService;

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        
    	Cookie[] cookies = request.getCookies();
        if (cookies == null) return;

        for (Cookie cookie : cookies) {
            if ("refreshToken".equals(cookie.getName())) {
                String refreshToken = cookie.getValue();
                //revoke + historyId 가져오기
                Long historyId = jwtService.revokeRefresh(refreshToken);
                // 로그인 이력 업데이트
                if (historyId != null) {
                    loginHistoryService.logout(historyId);
                }
                // 쿠키 삭제
                cookieService.clearRefreshCookie(response);
            }
        }
        
    }

    
}
