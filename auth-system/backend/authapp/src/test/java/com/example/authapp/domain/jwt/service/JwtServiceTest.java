package com.example.authapp.domain.jwt.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.example.authapp.domain.jwt.entity.RefreshTokenEntity;
import com.example.authapp.domain.jwt.exception.JwtException;
import com.example.authapp.domain.risk.service.RiskService;
import com.example.authapp.domain.authorization.entity.RoleEntity;
import com.example.authapp.domain.user.entity.SocialProviderType;
import com.example.authapp.domain.user.entity.UserEntity;
import com.example.authapp.domain.user.service.UserQueryService;
import com.example.authapp.util.JWTUtil;

import jakarta.servlet.http.Cookie;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private CookieService cookieService;

    @Mock
    private com.example.authapp.domain.audit.service.AuthEventLogService securityEventService;

    @Mock
    private RiskService riskService;

    @Mock
    private UserQueryService userQueryService;

    @InjectMocks
    private JwtService jwtService;

    @Test
    void exchangeUsesDatabaseRefreshTokenAndRiskValidationBeforeRotating() {
        String oldRefreshToken = JWTUtil.createJWT("socialUser", Set.of("ROLE_USER"), UUID.randomUUID().toString(), false);
        RefreshTokenEntity oldEntity = RefreshTokenEntity.builder()
                .username("socialUser")
                .refresh(oldRefreshToken)
                .jti(JWTUtil.getJti(oldRefreshToken))
                .expiresAt(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .ipAddress("127.0.0.1")
                .userAgent("JUnit")
                .device("JUnit")
                .build();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("refreshToken", oldRefreshToken));
        request.addHeader("User-Agent", "JUnit");
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(refreshTokenService.findByRefresh(oldRefreshToken)).thenReturn(oldEntity);
        when(riskService.analyzeTokenRisk(eq(oldEntity), any(), any(), any())).thenReturn(true);
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

        var result = jwtService.cookie2Header(request, response);

        assertThat(result.accessToken()).isNotBlank();
        assertThat(result.user().getUsername()).isEqualTo("socialUser");
        verify(refreshTokenService).findByRefresh(oldRefreshToken);
        verify(riskService).analyzeTokenRisk(eq(oldEntity), any(), any(), any());
        verify(refreshTokenService).save(any(RefreshTokenEntity.class));
        verify(cookieService).addRefreshCookie(eq(response), any());
    }

    @Test
    void exchangeRejectsRevokedOrHighRiskRefreshToken() {
        String oldRefreshToken = JWTUtil.createJWT("socialUser", Set.of("ROLE_USER"), UUID.randomUUID().toString(), false);
        RefreshTokenEntity oldEntity = RefreshTokenEntity.builder()
                .username("socialUser")
                .refresh(oldRefreshToken)
                .jti(JWTUtil.getJti(oldRefreshToken))
                .expiresAt(LocalDateTime.now().plusDays(7))
                .revoked(true)
                .build();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("refreshToken", oldRefreshToken));
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(refreshTokenService.findByRefresh(oldRefreshToken)).thenReturn(oldEntity);
        when(riskService.analyzeTokenRisk(eq(oldEntity), any(), any(), any())).thenReturn(false);

        assertThatThrownBy(() -> jwtService.cookie2Header(request, response))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void refreshRotateStoresCurrentRequestClientInfoInNewRefreshToken() {
        String oldRefreshToken = JWTUtil.createJWT("user1", Set.of("ROLE_USER"), UUID.randomUUID().toString(), false);
        RefreshTokenEntity oldEntity = RefreshTokenEntity.builder()
                .username("user1")
                .refresh(oldRefreshToken)
                .jti(JWTUtil.getJti(oldRefreshToken))
                .expiresAt(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .ipAddress("127.0.0.1")
                .userAgent("OldAgent")
                .device("OldDevice")
                .build();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("refreshToken", oldRefreshToken));
        request.setRemoteAddr("203.0.113.10");
        request.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0) AppleWebKit Chrome/120");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(refreshTokenService.findByRefresh(oldRefreshToken)).thenReturn(oldEntity);
        when(riskService.analyzeTokenRisk(eq(oldEntity), eq("203.0.113.10"), eq("Windows / Chrome"), any()))
                .thenReturn(true);
        when(userQueryService.getByUsername("user1")).thenReturn(UserEntity.builder()
                .username("user1")
                .email("user1@example.com")
                .nickname("user1")
                .enabled(true)
                .locked(false)
                .social(false)
                .build());

        jwtService.refreshRotate(request, response);

        ArgumentCaptor<RefreshTokenEntity> newTokenCaptor = ArgumentCaptor.forClass(RefreshTokenEntity.class);
        verify(refreshTokenService).save(newTokenCaptor.capture());
        RefreshTokenEntity newToken = newTokenCaptor.getValue();
        assertThat(newToken.getIpAddress()).isEqualTo("203.0.113.10");
        assertThat(newToken.getUserAgent()).isEqualTo("Mozilla/5.0 (Windows NT 10.0) AppleWebKit Chrome/120");
        assertThat(newToken.getDevice()).isEqualTo("Windows / Chrome");
    }

    @Test
    void refreshRotateUsesCurrentDatabaseRolesForNewAccessToken() {
        String oldRefreshToken = JWTUtil.createJWT("user1", Set.of("ROLE_USER"), UUID.randomUUID().toString(), false);
        RefreshTokenEntity oldEntity = RefreshTokenEntity.builder()
                .username("user1")
                .refresh(oldRefreshToken)
                .jti(JWTUtil.getJti(oldRefreshToken))
                .expiresAt(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .build();
        RoleEntity adminRole = RoleEntity.builder()
                .name("ROLE_ADMIN")
                .enabled(true)
                .build();
        UserEntity user = UserEntity.builder()
                .username("user1")
                .email("user1@example.com")
                .nickname("user1")
                .enabled(true)
                .locked(false)
                .social(false)
                .build();
        user.addRole(adminRole);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("refreshToken", oldRefreshToken));
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(refreshTokenService.findByRefresh(oldRefreshToken)).thenReturn(oldEntity);
        when(riskService.analyzeTokenRisk(eq(oldEntity), any(), any(), any())).thenReturn(true);
        when(userQueryService.getByUsername("user1")).thenReturn(user);

        var result = jwtService.refreshRotate(request, response);

        assertThat(JWTUtil.getRoles(result.accessToken())).containsExactly("ROLE_ADMIN");
        assertThat(result.user().getRoles()).containsExactly("ROLE_ADMIN");
    }

    @Test
    void refreshRotateRejectsDisabledAccountBeforeIssuingNewTokens() {
        String oldRefreshToken = JWTUtil.createJWT("user1", Set.of("ROLE_USER"), UUID.randomUUID().toString(), false);
        RefreshTokenEntity oldEntity = RefreshTokenEntity.builder()
                .username("user1")
                .refresh(oldRefreshToken)
                .jti(JWTUtil.getJti(oldRefreshToken))
                .expiresAt(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .build();
        UserEntity disabledUser = UserEntity.builder()
                .username("user1")
                .email("user1@example.com")
                .nickname("user1")
                .enabled(false)
                .locked(false)
                .social(false)
                .build();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("refreshToken", oldRefreshToken));
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(refreshTokenService.findByRefresh(oldRefreshToken)).thenReturn(oldEntity);
        when(riskService.analyzeTokenRisk(eq(oldEntity), any(), any(), any())).thenReturn(true);
        when(userQueryService.getByUsername("user1")).thenReturn(disabledUser);

        assertThatThrownBy(() -> jwtService.refreshRotate(request, response))
                .isInstanceOf(JwtException.class);
        verify(refreshTokenService, never()).save(any(RefreshTokenEntity.class));
    }
    
    
}
