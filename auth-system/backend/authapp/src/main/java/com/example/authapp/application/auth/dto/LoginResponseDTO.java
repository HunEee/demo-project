package com.example.authapp.application.auth.dto;

import com.example.authapp.security.handler.dto.UserResponseDTO;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponseDTO {

    private String accessToken;
    private long expiresIn;
    private UserResponseDTO user;
    
}
