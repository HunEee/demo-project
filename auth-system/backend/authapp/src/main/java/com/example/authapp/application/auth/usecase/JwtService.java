package com.example.authapp.application.auth.usecase;

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
import com.example.authapp.domain.jwt.service.JwtTokenProvider;
import com.example.authapp.domain.jwt.service.RefreshTokenService;
import com.example.authapp.domain.jwt.service.TokenSettingsService;
import com.example.authapp.domain.user.entity.UserEntity;
import com.example.authapp.domain.user.service.UserQueryService;
import com.example.authapp.application.risk.usecase.RiskService;
import com.example.authapp.security.handler.dto.UserResponseDTO;
import com.example.authapp.security.rbac.RbacAuthorizationService;

import io.jsonwebtoken.ExpiredJwtException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JwtService {

    public record JwtRotationResult(JWTResponseDTO response, String refreshToken) {
    }

    private final RefreshTokenService refreshTokenService;
    private final UserQueryService userQueryService;
    private final AuthEventLogService securityEventService;
    private final RiskService riskService;
    private final RbacAuthorizationService rbacAuthorizationService;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenSettingsService tokenSettingsService;

    @Transactional
    public JwtRotationResult cookie2Header(String refreshToken, String ip, String userAgent, String device) {
        validateRefreshToken(refreshToken);
        RefreshTokenEntity session = refreshTokenService.findByRefreshForUpdate(refreshToken);

        // 소셜 로그인 cookie 교환 흐름에서 refresh token을 회전하고 access token을 발급한다.
        JwtRotationResult result = rotateOrRecover(refreshToken, session, ip, userAgent, device, true, "OAUTH_COOKIE_EXCHANGE");
        if (result.response().accessToken() == null) {
            throw JwtException.revokedRefreshToken();
        }
        return result;
    }

    @Transactional
    public JwtRotationResult refreshRotate(String refreshToken, String ip, String userAgent, String device, String refreshReason) {
        String username = "UNKNOWN";
        try {
            validateRefreshToken(refreshToken);
            RefreshTokenEntity session = refreshTokenService.findByRefreshForUpdate(refreshToken);
            username = session.getUsername();

            // refresh token을 회전하고 새 access token을 발급한다.
            JwtRotationResult result = rotateOrRecover(refreshToken, session, ip, userAgent, device, false, refreshReason);
            if (result.response().accessToken() == null) {
                securityEventService.tokenReissueFail(username, "Refresh token unavailable");
                return result;
            }
            securityEventService.tokenReissue(session.getUsername());
            return result;
        } catch (RuntimeException e) {
            securityEventService.tokenReissueFail(username, e.getMessage());
            throw e;
        }
    }

    private JwtRotationResult rotateOrRecover(
            String refreshToken,
            RefreshTokenEntity session,
            String ip,
            String userAgent,
            String device,
            boolean throwOnFailure,
            String refreshReason
    ) {
        LocalDateTime now = LocalDateTime.now();
        if (session.isRecentlyRotated(now)) {
            return recoverRecentRotation(session);
        }

        // 토큰 재사용 위험도를 검사하고 사용할 수 없으면 회전을 중단한다.
        boolean available = riskService.analyzeTokenRisk(session, ip, device, userAgent);
        if (!available) {
            if (throwOnFailure) {
                throw JwtException.revokedRefreshToken();
            }
            return new JwtRotationResult(new JWTResponseDTO(null), null);
        }

        session.markUsed();
        return rotateRefreshToken(session, ip, userAgent, device, now, resolveRotationReason(refreshReason));
    }

    private JwtRotationResult rotateRefreshToken(
            RefreshTokenEntity oldEntity,
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
        String newRefreshToken = jwtTokenProvider.createToken(user.getUsername(), roles, newJti, false);

        // 기존 refresh token에 grace period와 대체 jti를 기록한다.
        int rotationGraceSeconds = tokenSettingsService.current().getRotationGraceSeconds();
        oldEntity.rotateTo(newJti, rotationReason, now, now.plusSeconds(rotationGraceSeconds));

        RefreshTokenEntity newEntity = RefreshTokenEntity.builder()
                .username(user.getUsername())
                .refreshTokenHash(RefreshTokenService.hashToken(newRefreshToken))
                .jti(newJti)
                .familyId(oldEntity.getFamilyId())
                .tokenSequence(oldEntity.getTokenSequence() + 1)
                .expiresAt(LocalDateTime.now().plusDays(tokenSettingsService.current().getRefreshTokenLifetimeDays()))
                .ipAddress(ip)
                .userAgent(userAgent)
                .device(device)
                .revoked(false)
                .loginHistory(oldEntity.getLoginHistory())
                .build();

        refreshTokenService.save(newEntity);
        return new JwtRotationResult(issueAccessToken(newEntity, user, roles), newRefreshToken);
    }

    private JwtRotationResult recoverRecentRotation(RefreshTokenEntity oldEntity) {
        RefreshTokenEntity replacement = refreshTokenService
                .findActiveReplacement(oldEntity.getFamilyId(), oldEntity.getReplacedByToken())
                .orElseThrow(JwtException::revokedRefreshToken);
        return new JwtRotationResult(issueAccessToken(replacement), null);
    }

    private JWTResponseDTO issueAccessToken(RefreshTokenEntity session) {
        String username = session.getUsername();
        UserEntity user = userQueryService.getByUsername(username);
        validateAccountAvailable(user);
        return issueAccessToken(session, user, currentRoles(user));
    }

    private JWTResponseDTO issueAccessToken(RefreshTokenEntity session, UserEntity user, Set<String> roles) {
        String accessToken = jwtTokenProvider.createToken(user.getUsername(), roles, session.getJti(), true);
        Set<String> permissions = rbacAuthorizationService.findEffectivePermissions(user.getUsername());

        return new JWTResponseDTO(
                accessToken,
                UserResponseDTO.from(user, roles, permissions),
                jwtTokenProvider.accessTokenExpiresInSeconds()
        );
    }

    private Set<String> currentRoles(UserEntity user) {
        return user.getRoles()
                .stream()
                .map(RoleEntity::getName)
                .collect(Collectors.toSet());
    }

    private String resolveRotationReason(String reason) {
        if (reason == null || reason.isBlank()) {
            reason = "UNKNOWN";
        }
        // access token 누락으로 인한 회전 사유를 기록한다.
        if ("ACCESS_TOKEN_MISSING".equals(reason)) {
            return "ROTATED_ACCESS_TOKEN_MISSING";
        }
        // access token 만료로 인한 회전 사유를 기록한다.
        if ("ACCESS_TOKEN_EXPIRED".equals(reason)) {
            return "ROTATED_ACCESS_TOKEN_EXPIRED";
        }
        // 소셜 로그인 cookie 교환으로 인한 회전 사유를 기록한다.
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
            jwtTokenProvider.validate(refreshToken, false);
        } catch (ExpiredJwtException e) {
            throw JwtException.expiredRefreshToken();
        } catch (io.jsonwebtoken.JwtException e) {
            throw JwtException.invalidRefreshToken();
        }
    }
}
