package com.example.authapp.domain.risk.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.authapp.domain.audit.repository.LoginHistoryRepository;
import com.example.authapp.domain.jwt.entity.RefreshTokenEntity;
import com.example.authapp.domain.risk.entity.RiskEntity;
import com.example.authapp.domain.risk.entity.RiskLevel;
import com.example.authapp.domain.risk.repository.RiskRepository;
import com.example.authapp.domain.user.entity.UserEntity;
import com.example.authapp.domain.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class RiskServiceTest {

    @Mock
    private RiskRepository riskRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LoginHistoryRepository loginHistoryRepository;

    @Mock
    private RiskFactory riskFactory;

    @Mock
    private RiskEvaluator riskEvaluator;

    @Mock
    private RiskEventService riskEventService;

    @Mock
    private RiskActionService riskActionService;

    @InjectMocks
    private RiskService riskService;

    @Test
    void expectedRevokedSessionUseDoesNotTriggerTokenReuseResponse() {
        UserEntity user = UserEntity.builder()
                .username("user1")
                .email("user1@example.com")
                .enabled(true)
                .locked(false)
                .build();
        RiskEntity risk = RiskEntity.builder()
                .username("user1")
                .riskScore(0)
                .riskLevel(RiskLevel.LOW)
                .build();
        RefreshTokenEntity token = RefreshTokenEntity.builder()
                .username("user1")
                .refreshTokenHash("hash")
                .jti("jti")
                .familyId("jti")
                .tokenSequence(0L)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .build();
        token.revokeBy("USER_LOGOUT_OTHER_DEVICE", "user1");

        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(user));
        when(riskRepository.findByUserUsername("user1")).thenReturn(Optional.of(risk));

        boolean result = riskService.analyzeTokenRisk(token, "127.0.0.1", "Chrome", "JUnit");

        assertThat(result).isFalse();
        verify(riskEventService).saveTokenRisk("user1", 0, "REVOKED_SESSION_USED", "127.0.0.1", "Chrome");
        verify(riskActionService, never()).tokenReuseDetected(any(), any(), any());
    }

    @Test
    void unexpectedRevokedTokenUseTriggersTokenReuseResponse() {
        UserEntity user = UserEntity.builder()
                .username("user1")
                .email("user1@example.com")
                .enabled(true)
                .locked(false)
                .build();
        RiskEntity risk = RiskEntity.builder()
                .username("user1")
                .riskScore(0)
                .riskLevel(RiskLevel.LOW)
                .build();
        RefreshTokenEntity token = RefreshTokenEntity.builder()
                .username("user1")
                .refreshTokenHash("hash")
                .jti("jti")
                .familyId("jti")
                .tokenSequence(0L)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .build();
        token.revokeBy("ROTATED_ACCESS_TOKEN_EXPIRED", "SYSTEM");

        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(user));
        when(riskRepository.findByUserUsername("user1")).thenReturn(Optional.of(risk));

        boolean result = riskService.analyzeTokenRisk(token, "127.0.0.1", "Chrome", "JUnit");

        assertThat(result).isFalse();
        verify(riskEventService).saveCritical("user1", "TOKEN_REUSE", "127.0.0.1", "Chrome");
        verify(riskActionService).tokenReuseDetected(eq("user1"), eq("127.0.0.1"), eq("Chrome"));
        assertThat(token.getReuseDetectedAt()).isNotNull();
    }

    @Test
    void rotatedTokenAfterGraceTriggersTokenReuseResponse() {
        UserEntity user = UserEntity.builder()
                .username("user1")
                .email("user1@example.com")
                .enabled(true)
                .locked(false)
                .build();
        RiskEntity risk = RiskEntity.builder()
                .username("user1")
                .riskScore(0)
                .riskLevel(RiskLevel.LOW)
                .build();
        RefreshTokenEntity token = RefreshTokenEntity.builder()
                .username("user1")
                .refreshTokenHash("hash")
                .jti("jti")
                .familyId("jti")
                .tokenSequence(0L)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .build();
        token.rotateTo("replacement-jti", LocalDateTime.now().minusMinutes(1), LocalDateTime.now().minusSeconds(1));

        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(user));
        when(riskRepository.findByUserUsername("user1")).thenReturn(Optional.of(risk));

        boolean result = riskService.analyzeTokenRisk(token, "127.0.0.1", "Chrome", "JUnit");

        assertThat(result).isFalse();
        verify(riskEventService).saveCritical("user1", "TOKEN_REUSE", "127.0.0.1", "Chrome");
        verify(riskActionService).tokenReuseDetected(eq("user1"), eq("127.0.0.1"), eq("Chrome"));
        assertThat(token.getReuseDetectedAt()).isNotNull();
    }
}
