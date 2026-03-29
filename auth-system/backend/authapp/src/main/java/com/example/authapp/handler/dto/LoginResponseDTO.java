package com.example.authapp.handler.dto;

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
