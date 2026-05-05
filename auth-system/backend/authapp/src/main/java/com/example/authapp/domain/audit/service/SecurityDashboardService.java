package com.example.authapp.domain.audit.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.authapp.domain.audit.dto.SecurityStatusResponseDTO;
import com.example.authapp.domain.audit.repository.SecurityIncidentRepository;
import com.example.authapp.domain.jwt.entity.RefreshTokenEntity;
import com.example.authapp.domain.jwt.repository.RefreshTokenRepository;
import com.example.authapp.domain.jwt.service.JwtService;
import com.example.authapp.util.JWTUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SecurityDashboardService {

    private final RefreshTokenRepository refreshRepository;
    private final SecurityIncidentRepository securityIncidentRepository;
    
    @Transactional(readOnly = true)
    public SecurityStatusResponseDTO getSecurityStatus(String username) {

        List<RefreshTokenEntity> tokens = refreshRepository.findByUsername(username);

        if (tokens.isEmpty()) {
            return new SecurityStatusResponseDTO(null, null, null, "SAFE");
        }

        // 최신 토큰 기준
        RefreshTokenEntity latest = tokens.stream()
                .max((a, b) -> a.getExpiresAt().compareTo(b.getExpiresAt()))
                .orElseThrow();
        
        // 토큰 만료시간 계싼
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime accessExpire = now.plusSeconds(JWTUtil.getAccessTokenExpiresIn());
        LocalDateTime refreshExpire = latest.getExpiresAt();

        // 상태 판단 로직
        String status = "SAFE";
        
        if (tokens.stream().anyMatch(RefreshTokenEntity::isRevoked)) { // 1. 탈취 감지 
            status = "DANGER";
        } else if (refreshExpire.isBefore(now.plusDays(1))) {// 2. 만료 임박
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