package com.example.authapp.application.auth.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.example.authapp.domain.mfa.entity.MfaMethodType;
import com.example.authapp.security.handler.dto.UserResponseDTO;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponseDTO {

    private boolean mfaRequired;
    private boolean mfaRegistrationRequired;
    private String challengeId;
    private LocalDateTime mfaExpiresAt;
    private List<MfaMethodType> availableMethods;
    private String accessToken;
    private long expiresIn;
    private UserResponseDTO user;
}
