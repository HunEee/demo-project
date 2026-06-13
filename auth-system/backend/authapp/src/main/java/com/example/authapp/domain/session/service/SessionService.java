package com.example.authapp.domain.session.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.authapp.domain.audit.dto.LoginHistoryResponse;
import com.example.authapp.domain.audit.service.AuthEventLogService;
import com.example.authapp.domain.audit.service.LoginHistoryService;
import com.example.authapp.domain.jwt.entity.RefreshTokenEntity;
import com.example.authapp.domain.jwt.service.RefreshTokenService;
import com.example.authapp.domain.session.dto.SessionResponse;
import com.example.authapp.util.JWTUtil;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final RefreshTokenService refreshTokenService;
    private final LoginHistoryService loginHistoryService;
    private final AuthEventLogService authEventLogService;

    public List<SessionResponse> getSessions(String username, HttpServletRequest request) {
        String currentJti = resolveCurrentJti(request);
        List<RefreshTokenEntity> tokens = refreshTokenService.findActiveByUsername(username);

        return tokens.stream()
                .map(token -> SessionResponse.builder()
                        .id(token.getId())
                        .ip(token.getIpAddress())
                        .device(token.getDevice())
                        .createdAt(resolveSessionStartTime(token))
                        .lastAccessAt(token.getLastUsedAt() != null
                                ? token.getLastUsedAt().toString()
                                : token.getCreatedAt().toString())
                        .current(token.getJti().equals(currentJti))
                        .build())
                .toList();
    }

    @Transactional
    public void logoutSession(Long id, String username) {
        RefreshTokenEntity refreshToken = refreshTokenService.findByIdAndUsername(id, username);
        refreshToken.revokeBy("USER_LOGOUT_OTHER_DEVICE", username);
        processLogout(refreshToken);
    }

    @Transactional
    public void logoutAll(String username, HttpServletRequest request) {
        String currentJti = resolveCurrentJti(request);
        refreshTokenService.findActiveByUsername(username)
                .stream()
                .filter(token -> !token.getJti().equals(currentJti))
                .forEach(token -> {
                    token.revokeBy("USER_LOGOUT_OTHER_DEVICES", username);
                    processLogout(token);
                });
    }

    private String resolveCurrentJti(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return JWTUtil.getJti(authorization.substring("Bearer ".length()));
        }
        return JWTUtil.getJti(extractRefreshToken(request));
    }

    private String extractRefreshToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            throw new IllegalStateException("refreshToken cookie not found");
        }
        for (Cookie cookie : cookies) {
            if ("refreshToken".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        throw new IllegalStateException("refreshToken cookie not found");
    }

    private LocalDateTime resolveSessionStartTime(RefreshTokenEntity token) {
        if (token.getLoginHistory() != null) {
            return token.getLoginHistory().getLoginAt();
        }
        return token.getCreatedAt();
    }

    private void processLogout(RefreshTokenEntity refreshToken) {
        if (refreshToken.getLoginHistory() == null) {
            return;
        }
        LoginHistoryResponse history = refreshToken.getLoginHistory().toResponse();
        loginHistoryService.logout(history);
        authEventLogService.logout(history.username());
    }
}
