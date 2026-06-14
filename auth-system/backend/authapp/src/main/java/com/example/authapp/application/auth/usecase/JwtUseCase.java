package com.example.authapp.application.auth.usecase;

import org.springframework.stereotype.Service;

import com.example.authapp.domain.jwt.dto.JWTResponseDTO;
import com.example.authapp.domain.jwt.exception.JwtException;
import com.example.authapp.util.ClientUtil;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JwtUseCase {

    private static final String REFRESH_REASON_HEADER = "X-Refresh-Reason";

    private final JwtService jwtService;
    private final CookieService cookieService;

    public JWTResponseDTO cookie2Header(HttpServletRequest request, HttpServletResponse response) {
        // refresh cookie에서 토큰을 추출한다.
        String refreshToken = extractRefreshToken(request);

        // 클라이언트 정보를 계산해서 JWT usecase에 전달한다.
        var result = jwtService.cookie2Header(
                refreshToken,
                ClientUtil.getIp(request),
                ClientUtil.getUserAgent(request),
                ClientUtil.getDevice(ClientUtil.getUserAgent(request))
        );

        // 새 refresh token이 발급되면 cookie에 반영한다.
        addRefreshCookie(response, result.refreshToken());
        return result.response();
    }

    public JWTResponseDTO refreshRotate(HttpServletRequest request, HttpServletResponse response) {
        // refresh cookie에서 토큰을 추출한다.
        String refreshToken = extractRefreshToken(request);

        // refresh token 회전 요청을 JWT usecase에 위임한다.
        var result = jwtService.refreshRotate(
                refreshToken,
                ClientUtil.getIp(request),
                ClientUtil.getUserAgent(request),
                ClientUtil.getDevice(ClientUtil.getUserAgent(request)),
                request.getHeader(REFRESH_REASON_HEADER)
        );

        // 재발급이 거부되면 HTTP 상태를 401로 설정한다.
        if (result.response().accessToken() == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }

        // 새 refresh token이 발급되면 cookie에 반영한다.
        addRefreshCookie(response, result.refreshToken());
        return result.response();
    }

    private void addRefreshCookie(HttpServletResponse response, String refreshToken) {
        // 비어 있지 않은 refresh token만 cookie로 내려준다.
        if (refreshToken != null && !refreshToken.isBlank()) {
            cookieService.addRefreshCookie(response, refreshToken);
        }
    }

    private String extractRefreshToken(HttpServletRequest request) {
        // 요청 cookie 목록에서 refresh token을 찾는다.
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
