package com.example.authapp.application.auth.dto;

import com.example.authapp.security.handler.dto.UserResponseDTO;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponseDTO {

    private String accessToken;
    private String refreshToken; // 나중에 제거 가능
    private long expiresIn;
    private UserResponseDTO user;
    
}
