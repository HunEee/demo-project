package com.example.authapp.domain.jwt.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.authapp.domain.audit.service.AuthEventLogService;
import com.example.authapp.domain.authorization.entity.RoleEntity;
import com.example.authapp.domain.jwt.dto.JWTResponseDTO;
import com.example.authapp.domain.jwt.entity.RefreshTokenEntity;
import com.example.authapp.domain.jwt.exception.JwtException;
import com.example.authapp.domain.risk.service.RiskService;
import com.example.authapp.domain.user.entity.UserEntity;
import com.example.authapp.domain.user.service.UserQueryService;
import com.example.authapp.security.handler.dto.UserResponseDTO;
import com.example.authapp.security.rbac.RbacAuthorizationService;
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

    private static final Duration ROTATION_GRACE = Duration.ofSeconds(30);
    private static final String REFRESH_REASON_HEADER = "X-Refresh-Reason";

    private final RefreshTokenService refreshTokenService;
    private final CookieService cookieService;
    private final UserQueryService userQueryService;
    private final AuthEventLogService securityEventService;
    private final RiskService riskService;
    private final RbacAuthorizationService rbacAuthorizationService;

    @Transactional
    public JWTResponseDTO cookie2Header(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = extractRefreshToken(request);
        validateRefreshToken(refreshToken);

        RefreshTokenEntity session = refreshTokenService.findByRefreshForUpdate(refreshToken);

        JWTResponseDTO result = rotateOrRecover(refreshToken, session, request, response, true, "OAUTH_COOKIE_EXCHANGE");
        if (result.accessToken() == null) {
            throw JwtException.revokedRefreshToken();
        }
        return result;
    }

    @Transactional
    public JWTResponseDTO refreshRotate(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = extractRefreshToken(request);
        validateRefreshToken(refreshToken);

        RefreshTokenEntity session = refreshTokenService.findByRefreshForUpdate(refreshToken);
        JWTResponseDTO result = rotateOrRecover(refreshToken, session, request, response, false, "UNKNOWN");
        securityEventService.tokenReissue(session.getUsername());
        return result;
    }

    private JWTResponseDTO rotateOrRecover(
            String refreshToken,
            RefreshTokenEntity session,
            HttpServletRequest request,
            HttpServletResponse response,
            boolean throwOnFailure,
            String defaultReason
    ) {
        LocalDateTime now = LocalDateTime.now();
        if (session.isRecentlyRotated(now)) {
            return recoverRecentRotation(session);
        }

        String ip = ClientUtil.getIp(request);
        String userAgent = ClientUtil.getUserAgent(request);
        String device = ClientUtil.getDevice(userAgent);

        boolean available = riskService.analyzeTokenRisk(session, ip, device, userAgent);
        if (!available) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            if (throwOnFailure) {
                throw JwtException.revokedRefreshToken();
            }
            return new JWTResponseDTO(null);
        }

        session.markUsed();
        return rotateRefreshToken(session, response, ip, userAgent, device, now, resolveRotationReason(request, defaultReason));
    }

    private JWTResponseDTO rotateRefreshToken(
            RefreshTokenEntity oldEntity,
            HttpServletResponse response,
            String ip,
            String userAgent,
            String device,
            LocalDateTime now,
            String rotationReason
    ) {
        UserEntity user = userQueryService.getByUsername(oldEntity.getUsername());
        validateAccountAvailable(user);
        Set<String> roles = currentRoles(user);
        String newJti = UUID.randomUUID().toString();
        String newRefreshToken = JWTUtil.createJWT(user.getUsername(), roles, newJti, false);

        oldEntity.rotateTo(newJti, rotationReason, now, now.plus(ROTATION_GRACE));

        RefreshTokenEntity newEntity = RefreshTokenEntity.builder()
                .username(user.getUsername())
                .refreshTokenHash(RefreshTokenService.hashToken(newRefreshToken))
                .jti(newJti)
                .familyId(oldEntity.getFamilyId())
                .tokenSequence(oldEntity.getTokenSequence() + 1)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .ipAddress(ip)
                .userAgent(userAgent)
                .device(device)
                .revoked(false)
                .loginHistory(oldEntity.getLoginHistory())
                .build();

        refreshTokenService.save(newEntity);
        cookieService.addRefreshCookie(response, newRefreshToken);
        return issueAccessToken(newEntity, user, roles);
    }

    private JWTResponseDTO recoverRecentRotation(RefreshTokenEntity oldEntity) {
        RefreshTokenEntity replacement = refreshTokenService
                .findActiveReplacement(oldEntity.getFamilyId(), oldEntity.getReplacedByToken())
                .orElseThrow(JwtException::revokedRefreshToken);
        return issueAccessToken(replacement);
    }

    private JWTResponseDTO issueAccessToken(RefreshTokenEntity session) {
        String username = session.getUsername();
        UserEntity user = userQueryService.getByUsername(username);
        validateAccountAvailable(user);
        return issueAccessToken(session, user, currentRoles(user));
    }

    private JWTResponseDTO issueAccessToken(RefreshTokenEntity session, UserEntity user, Set<String> roles) {
        String accessToken = JWTUtil.createJWT(user.getUsername(), roles, session.getJti(), true);
        Set<String> permissions = rbacAuthorizationService.findEffectivePermissions(user.getUsername());

        return new JWTResponseDTO(
                accessToken,
                UserResponseDTO.from(user, roles, permissions),
                JWTUtil.getAccessTokenExpiresIn()
        );
    }

    private Set<String> currentRoles(UserEntity user) {
        return user.getRoles()
                .stream()
                .map(RoleEntity::getName)
                .collect(Collectors.toSet());
    }

    private String resolveRotationReason(HttpServletRequest request, String defaultReason) {
        String reason = request.getHeader(REFRESH_REASON_HEADER);
        if (reason == null || reason.isBlank()) {
            reason = defaultReason;
        }
        // 새로고침으로 인한 엑세스 토큰 사라짐  
        if ("ACCESS_TOKEN_MISSING".equals(reason)) {
            return "ROTATED_ACCESS_TOKEN_MISSING";
        }
        // 엑세스 토큰 만료
        if ("ACCESS_TOKEN_EXPIRED".equals(reason)) {
            return "ROTATED_ACCESS_TOKEN_EXPIRED";
        }
        if ("OAUTH_COOKIE_EXCHANGE".equals(reason)) {
            return "ROTATED_OAUTH_COOKIE_EXCHANGE";
        }
        return "ROTATED_UNKNOWN";
    }

    private void validateAccountAvailable(UserEntity user) {
        if (user.isDeleted() || user.isLocked() || !user.isEnabled()) {
            throw JwtException.revokedRefreshToken();
        }
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
