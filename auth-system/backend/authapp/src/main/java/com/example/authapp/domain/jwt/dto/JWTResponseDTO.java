package com.example.authapp.domain.jwt.dto;

public record JWTResponseDTO(
		String accessToken, 
		String refreshToken
) {}
