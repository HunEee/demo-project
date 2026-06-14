package com.example.authapp.domain.jwt.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.authapp.domain.jwt.entity.RefreshTokenEntity;
import com.example.authapp.domain.jwt.entity.TokenSettingsEntity;
import com.example.authapp.domain.jwt.repository.RefreshTokenRepository;
import com.example.authapp.util.JWTUtil;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private TokenSettingsService tokenSettingsService;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    @Test
    void addRefreshStoresTokenHashInsteadOfPlainToken() {
        String refreshToken = JWTUtil.createJWT("user1", Set.of("ROLE_USER"), UUID.randomUUID().toString(), false);
        when(jwtTokenProvider.getJti(refreshToken)).thenReturn(JWTUtil.getJti(refreshToken));
        when(tokenSettingsService.current()).thenReturn(TokenSettingsEntity.defaults());

        refreshTokenService.addRefresh("user1", refreshToken, "127.0.0.1", "JUnit", "JUnit", null);

        ArgumentCaptor<RefreshTokenEntity> captor = ArgumentCaptor.forClass(RefreshTokenEntity.class);
        verify(refreshTokenRepository).save(captor.capture());
        assertThat(captor.getValue().getRefreshTokenHash()).isEqualTo(RefreshTokenService.hashToken(refreshToken));
        assertThat(captor.getValue().getFamilyId()).isEqualTo(JWTUtil.getJti(refreshToken));
        assertThat(captor.getValue().getTokenSequence()).isZero();
    }

    @Test
    void findByRefreshForUpdateUsesTokenHash() {
        String refreshToken = JWTUtil.createJWT("user1", Set.of("ROLE_USER"), UUID.randomUUID().toString(), false);
        RefreshTokenEntity entity = RefreshTokenEntity.builder()
                .username("user1")
                .refreshTokenHash(RefreshTokenService.hashToken(refreshToken))
                .jti(JWTUtil.getJti(refreshToken))
                .familyId(JWTUtil.getJti(refreshToken))
                .tokenSequence(0L)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByRefreshTokenHashForUpdate(RefreshTokenService.hashToken(refreshToken)))
                .thenReturn(Optional.of(entity));

        RefreshTokenEntity result = refreshTokenService.findByRefreshForUpdate(refreshToken);

        assertThat(result).isSameAs(entity);
    }

    @Test
    void revokeRefreshRecordsReasonAndActor() {
        String refreshToken = JWTUtil.createJWT("user1", Set.of("ROLE_USER"), UUID.randomUUID().toString(), false);
        RefreshTokenEntity entity = RefreshTokenEntity.builder()
                .username("user1")
                .refreshTokenHash(RefreshTokenService.hashToken(refreshToken))
                .jti(JWTUtil.getJti(refreshToken))
                .familyId(JWTUtil.getJti(refreshToken))
                .tokenSequence(0L)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByRefreshTokenHash(RefreshTokenService.hashToken(refreshToken)))
                .thenReturn(Optional.of(entity));

        refreshTokenService.revokeRefresh(refreshToken, "ADMIN_REVOKE", "admin");

        assertThat(entity.isRevoked()).isTrue();
        assertThat(entity.getRevokedReason()).isEqualTo("ADMIN_REVOKE");
        assertThat(entity.getRevokedBy()).isEqualTo("admin");
        assertThat(entity.getRevokedAt()).isNotNull();
    }

    @Test
    void revokeRefreshUsesTokenOwnerAsActorWhenActorIsNotProvided() {
        String refreshToken = JWTUtil.createJWT("user1", Set.of("ROLE_USER"), UUID.randomUUID().toString(), false);
        RefreshTokenEntity entity = RefreshTokenEntity.builder()
                .username("user1")
                .refreshTokenHash(RefreshTokenService.hashToken(refreshToken))
                .jti(JWTUtil.getJti(refreshToken))
                .familyId(JWTUtil.getJti(refreshToken))
                .tokenSequence(0L)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByRefreshTokenHash(RefreshTokenService.hashToken(refreshToken)))
                .thenReturn(Optional.of(entity));

        refreshTokenService.revokeRefresh(refreshToken);

        assertThat(entity.getRevokedReason()).isEqualTo("LOGOUT");
        assertThat(entity.getRevokedBy()).isEqualTo("user1");
    }

    @Test
    void revokeAllByUsernameUsesUsernameAsActor() {
        refreshTokenService.revokeAllByUsername("user1", "PASSWORD_CHANGED");

        verify(refreshTokenRepository).revokeActiveByUsername(
                eq("user1"),
                eq("PASSWORD_CHANGED"),
                eq("user1"),
                any(LocalDateTime.class)
        );
    }
}
