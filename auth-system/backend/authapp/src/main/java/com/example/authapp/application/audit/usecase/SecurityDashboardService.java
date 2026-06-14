package com.example.authapp.application.audit.usecase;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.authapp.domain.audit.dto.SecurityStatusResponseDTO;
import com.example.authapp.domain.audit.repository.SecurityIncidentRepository;
import com.example.authapp.domain.jwt.entity.RefreshTokenEntity;
import com.example.authapp.domain.jwt.repository.RefreshTokenRepository;
import com.example.authapp.domain.jwt.service.JwtTokenProvider;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SecurityDashboardService {

    private final RefreshTokenRepository refreshRepository;
    private final SecurityIncidentRepository securityIncidentRepository;
    private final JwtTokenProvider jwtTokenProvider;

    // 사용자 보안 상태 요약을 조회한다.
    @Transactional(readOnly = true)
    public SecurityStatusResponseDTO getSecurityStatus(String username) {
        List<RefreshTokenEntity> tokens = refreshRepository.findByUsername(username);
        if (tokens.isEmpty()) {
            return new SecurityStatusResponseDTO(null, null, null, "SAFE");
        }

        // 가장 늦게 만료되는 refresh token을 최신 세션 기준으로 사용한다.
        RefreshTokenEntity latest = tokens.stream()
                .max((a, b) -> a.getExpiresAt().compareTo(b.getExpiresAt()))
                .orElseThrow();

        // access token과 refresh token 만료 시간을 계산한다.
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime accessExpire = now.plusSeconds(jwtTokenProvider.accessTokenExpiresInSeconds());
        LocalDateTime refreshExpire = latest.getExpiresAt();

        // 폐기 또는 만료 임박 여부로 보안 상태를 판단한다.
        String status = "SAFE";
        if (tokens.stream().anyMatch(RefreshTokenEntity::isRevoked)) {
            status = "DANGER";
        } else if (refreshExpire.isBefore(now.plusDays(1))) {
            status = "WARNING";
        }

        return new SecurityStatusResponseDTO(
                accessExpire.toString(),
                refreshExpire.toString(),
                latest.getCreatedAt() != null ? latest.getCreatedAt().toString() : null,
                status
        );
    }
}
