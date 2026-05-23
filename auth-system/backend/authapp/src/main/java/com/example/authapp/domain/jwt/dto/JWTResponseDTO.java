package com.example.authapp.domain.jwt.dto;

import com.example.authapp.security.handler.dto.UserResponseDTO;

public record JWTResponseDTO(
		String accessToken,
		UserResponseDTO user,
		long expiresIn
) {
    public JWTResponseDTO(String accessToken) {
        this(accessToken, null, 0);
    }
}
