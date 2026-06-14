package com.example.authapp.domain.jwt.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import com.example.authapp.application.auth.usecase.JwtService;
import com.example.authapp.domain.authorization.entity.RoleEntity;
import com.example.authapp.domain.jwt.entity.RefreshTokenEntity;
import com.example.authapp.domain.jwt.entity.TokenSettingsEntity;
import com.example.authapp.domain.jwt.exception.JwtException;
import com.example.authapp.application.risk.usecase.RiskService;
import com.example.authapp.domain.user.entity.SocialProviderType;
import com.example.authapp.domain.user.entity.UserEntity;
import com.example.authapp.domain.user.service.UserQueryService;
import com.example.authapp.security.rbac.RbacAuthorizationService;
import com.example.authapp.util.JWTUtil;

import jakarta.servlet.http.Cookie;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private com.example.authapp.domain.audit.service.AuthEventLogService securityEventService;

    @Mock
    private RiskService riskService;

    @Mock
    private UserQueryService userQueryService;

    @Mock
    private RbacAuthorizationService rbacAuthorizationService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private TokenSettingsService tokenSettingsService;

    @InjectMocks
    private JwtService jwtService;

    @BeforeEach
    void setUpJwtProvider() {
        lenient().when(tokenSettingsService.current()).thenReturn(TokenSettingsEntity.defaults());
        lenient().when(jwtTokenProvider.accessTokenExpiresInSeconds()).thenReturn(3600L);
        lenient().when(jwtTokenProvider.createToken(any(), any(), any(), eq(true)))
                .thenAnswer(invocation -> JWTUtil.createJWT(
                        invocation.getArgument(0),
                        invocation.getArgument(1),
                        invocation.getArgument(2),
                        true
                ));
        lenient().when(jwtTokenProvider.createToken(any(), any(), any(), eq(false)))
                .thenAnswer(invocation -> JWTUtil.createJWT(
                        invocation.getArgument(0),
                        invocation.getArgument(1),
                        invocation.getArgument(2),
                        false
                ));
    }

    @Test
    void exchangeUsesDatabaseRefreshTokenAndRiskValidationBeforeIssuingAccessToken() {
        String refreshToken = JWTUtil.createJWT("socialUser", Set.of("ROLE_USER"), UUID.randomUUID().toString(), false);
        RefreshTokenEntity session = activeSession("socialUser", refreshToken);
        MockHttpServletRequest request = requestWithRefresh(refreshToken);
        request.addHeader("X-Refresh-Reason", "OAUTH_COOKIE_EXCHANGE");

        when(refreshTokenService.findByRefreshForUpdate(refreshToken)).thenReturn(session);
        when(riskService.analyzeTokenRisk(eq(session), any(), any(), any())).thenReturn(true);
        when(rbacAuthorizationService.findEffectivePermissions("socialUser")).thenReturn(Set.of("ADMIN_USERS_READ"));
        when(userQueryService.getByUsername("socialUser")).thenReturn(UserEntity.builder()
                .username("socialUser")
                .email("social@example.com")
                .nickname("social")
                .enabled(true)
                .locked(false)
                .social(true)
                .socialProviderType(SocialProviderType.NAVER)
                .providerId("12345")
                .build());

        var result = jwtService.cookie2Header(refreshToken, "127.0.0.1", "JUnit", "JUnit").response();

        assertThat(result.accessToken()).isNotBlank();
        assertThat(result.user().getUsername()).isEqualTo("socialUser");
        assertThat(result.user().getPermissions()).containsExactly("ADMIN_USERS_READ");
        assertThat(session.getLastUsedAt()).isNotNull();
        assertThat(session.isRevoked()).isTrue();
        assertThat(session.getRevokedReason()).isEqualTo("ROTATED_OAUTH_COOKIE_EXCHANGE");
        assertThat(session.getReplacedByToken()).isNotBlank();
        verify(refreshTokenService).findByRefreshForUpdate(refreshToken);
        verify(riskService).analyzeTokenRisk(eq(session), any(), any(), any());
        verify(refreshTokenService).save(any(RefreshTokenEntity.class));
    }

    @Test
    void exchangeRejectsRevokedOrHighRiskRefreshToken() {
        String refreshToken = JWTUtil.createJWT("socialUser", Set.of("ROLE_USER"), UUID.randomUUID().toString(), false);
        RefreshTokenEntity session = activeSession("socialUser", refreshToken);
        session.revokeBy("TOKEN_REUSE_DETECTED", "SYSTEM");
        MockHttpServletRequest request = requestWithRefresh(refreshToken);

        when(refreshTokenService.findByRefreshForUpdate(refreshToken)).thenReturn(session);
        when(riskService.analyzeTokenRisk(eq(session), any(), any(), any())).thenReturn(false);

        assertThatThrownBy(() -> jwtService.cookie2Header(refreshToken, "127.0.0.1", "JUnit", "JUnit"))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void refreshUsesCurrentRequestClientInfoForRiskValidation() {
        String refreshToken = JWTUtil.createJWT("user1", Set.of("ROLE_USER"), UUID.randomUUID().toString(), false);
        RefreshTokenEntity session = activeSession("user1", refreshToken);
        MockHttpServletRequest request = requestWithRefresh(refreshToken);
        request.setRemoteAddr("203.0.113.10");
        request.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0) AppleWebKit Chrome/120");
        request.addHeader("X-Refresh-Reason", "ACCESS_TOKEN_EXPIRED");

        when(refreshTokenService.findByRefreshForUpdate(refreshToken)).thenReturn(session);
        when(riskService.analyzeTokenRisk(eq(session), eq("203.0.113.10"), eq("Windows / Chrome"), any()))
                .thenReturn(true);
        when(rbacAuthorizationService.findEffectivePermissions("user1")).thenReturn(Set.of());
        when(userQueryService.getByUsername("user1")).thenReturn(availableUser("user1"));

        jwtService.refreshRotate(refreshToken, "203.0.113.10", request.getHeader("User-Agent"), "Windows / Chrome", "ACCESS_TOKEN_EXPIRED");

        verify(riskService).analyzeTokenRisk(eq(session), eq("203.0.113.10"), eq("Windows / Chrome"), any());
        verify(refreshTokenService).save(any(RefreshTokenEntity.class));
    }

    @Test
    void refreshRotatesRefreshTokenAndCreatesNewSessionInSameFamily() {
        String refreshToken = JWTUtil.createJWT("user1", Set.of("ROLE_USER"), UUID.randomUUID().toString(), false);
        RefreshTokenEntity session = activeSession("user1", refreshToken);
        MockHttpServletRequest request = requestWithRefresh(refreshToken);
        request.addHeader("X-Refresh-Reason", "ACCESS_TOKEN_MISSING");

        when(refreshTokenService.findByRefreshForUpdate(refreshToken)).thenReturn(session);
        when(riskService.analyzeTokenRisk(eq(session), any(), any(), any())).thenReturn(true);
        when(rbacAuthorizationService.findEffectivePermissions("user1")).thenReturn(Set.of());
        when(userQueryService.getByUsername("user1")).thenReturn(availableUser("user1"));

        var result = jwtService.refreshRotate(refreshToken, "127.0.0.1", "JUnit", "JUnit", "ACCESS_TOKEN_MISSING").response();

        ArgumentCaptor<RefreshTokenEntity> captor = ArgumentCaptor.forClass(RefreshTokenEntity.class);
        verify(refreshTokenService).save(captor.capture());
        RefreshTokenEntity replacement = captor.getValue();

        assertThat(result.accessToken()).isNotBlank();
        assertThat(JWTUtil.getJti(result.accessToken())).isEqualTo(replacement.getJti());
        assertThat(session.isRevoked()).isTrue();
        assertThat(session.getRevokedReason()).isEqualTo("ROTATED_ACCESS_TOKEN_MISSING");
        assertThat(session.getReplacedByToken()).isEqualTo(replacement.getJti());
        assertThat(session.getRotationGraceUntil()).isNotNull();
        assertThat(replacement.getFamilyId()).isEqualTo(session.getFamilyId());
        assertThat(replacement.getTokenSequence()).isEqualTo(session.getTokenSequence() + 1);
    }

    @Test
    void refreshRecoversRecentRotatedTokenWithoutCreatingAnotherSession() {
        String refreshToken = JWTUtil.createJWT("user1", Set.of("ROLE_USER"), UUID.randomUUID().toString(), false);
        RefreshTokenEntity session = activeSession("user1", refreshToken);
        session.rotateTo("replacement-jti", LocalDateTime.now(), LocalDateTime.now().plusSeconds(30));
        RefreshTokenEntity replacement = RefreshTokenEntity.builder()
                .username("user1")
                .refreshTokenHash("replacement-hash")
                .jti("replacement-jti")
                .familyId(session.getFamilyId())
                .tokenSequence(session.getTokenSequence() + 1)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .build();
        MockHttpServletRequest request = requestWithRefresh(refreshToken);

        when(refreshTokenService.findByRefreshForUpdate(refreshToken)).thenReturn(session);
        when(refreshTokenService.findActiveReplacement(session.getFamilyId(), "replacement-jti"))
                .thenReturn(java.util.Optional.of(replacement));
        when(rbacAuthorizationService.findEffectivePermissions("user1")).thenReturn(Set.of());
        when(userQueryService.getByUsername("user1")).thenReturn(availableUser("user1"));

        var result = jwtService.refreshRotate(refreshToken, "127.0.0.1", "JUnit", "JUnit", null).response();

        assertThat(result.accessToken()).isNotBlank();
        assertThat(JWTUtil.getJti(result.accessToken())).isEqualTo("replacement-jti");
        verify(riskService, never()).analyzeTokenRisk(any(), any(), any(), any());
        verify(refreshTokenService, never()).save(any(RefreshTokenEntity.class));
    }

    @Test
    void refreshUsesCurrentDatabaseRolesForNewAccessToken() {
        String refreshToken = JWTUtil.createJWT("user1", Set.of("ROLE_USER"), UUID.randomUUID().toString(), false);
        RefreshTokenEntity session = activeSession("user1", refreshToken);
        RoleEntity adminRole = RoleEntity.builder()
                .name("ROLE_ADMIN")
                .enabled(true)
                .build();
        UserEntity user = availableUser("user1");
        user.addRole(adminRole);
        MockHttpServletRequest request = requestWithRefresh(refreshToken);

        when(refreshTokenService.findByRefreshForUpdate(refreshToken)).thenReturn(session);
        when(riskService.analyzeTokenRisk(eq(session), any(), any(), any())).thenReturn(true);
        when(rbacAuthorizationService.findEffectivePermissions("user1")).thenReturn(Set.of("ADMIN_ROLES_READ"));
        when(userQueryService.getByUsername("user1")).thenReturn(user);

        var result = jwtService.refreshRotate(refreshToken, "127.0.0.1", "JUnit", "JUnit", null).response();

        assertThat(JWTUtil.getRoles(result.accessToken())).containsExactly("ROLE_ADMIN");
        assertThat(result.user().getRoles()).containsExactly("ROLE_ADMIN");
        assertThat(result.user().getPermissions()).containsExactly("ADMIN_ROLES_READ");
    }

    @Test
    void refreshRejectsDisabledAccountBeforeIssuingNewAccessToken() {
        String refreshToken = JWTUtil.createJWT("user1", Set.of("ROLE_USER"), UUID.randomUUID().toString(), false);
        RefreshTokenEntity session = activeSession("user1", refreshToken);
        UserEntity disabledUser = UserEntity.builder()
                .username("user1")
                .email("user1@example.com")
                .nickname("user1")
                .enabled(false)
                .locked(false)
                .social(false)
                .build();
        MockHttpServletRequest request = requestWithRefresh(refreshToken);

        when(refreshTokenService.findByRefreshForUpdate(refreshToken)).thenReturn(session);
        when(riskService.analyzeTokenRisk(eq(session), any(), any(), any())).thenReturn(true);
        when(userQueryService.getByUsername("user1")).thenReturn(disabledUser);

        assertThatThrownBy(() -> jwtService.refreshRotate(refreshToken, "127.0.0.1", "JUnit", "JUnit", null))
                .isInstanceOf(JwtException.class);
        verify(refreshTokenService, never()).save(any(RefreshTokenEntity.class));
    }

    private RefreshTokenEntity activeSession(String username, String refreshToken) {
        return RefreshTokenEntity.builder()
                .username(username)
                .refreshTokenHash(RefreshTokenService.hashToken(refreshToken))
                .jti(JWTUtil.getJti(refreshToken))
                .familyId(JWTUtil.getJti(refreshToken))
                .tokenSequence(0L)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .ipAddress("127.0.0.1")
                .userAgent("JUnit")
                .device("JUnit")
                .build();
    }

    private MockHttpServletRequest requestWithRefresh(String refreshToken) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("refreshToken", refreshToken));
        request.addHeader("User-Agent", "JUnit");
        request.setRemoteAddr("127.0.0.1");
        return request;
    }

    private UserEntity availableUser(String username) {
        return UserEntity.builder()
                .username(username)
                .email(username + "@example.com")
                .nickname(username)
                .enabled(true)
                .locked(false)
                .social(false)
                .build();
    }
}
