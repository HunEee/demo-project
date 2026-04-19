package com.example.authapp.domain.audit.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SecurityStatusResponseDTO {
    
	private String accessTokenExpiresAt;
    private String refreshTokenExpiresAt;
    private String lastRefreshedAt;
    private String status; // SAFE / WARNING / DANGER
    
}
