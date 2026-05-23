package com.example.authapp.domain.jwt.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

        var result = jwtService.cookie2Header(request, response);

        assertThat(result.accessToken()).isNotBlank();
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
    
    
}
