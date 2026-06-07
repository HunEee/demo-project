package com.example.authapp.domain.mfa.dto;

import java.time.LocalDateTime;

import com.example.authapp.domain.mfa.entity.MfaMethodType;

public record AdminMfaUserResponse(
        String username,
        String email,
        boolean mfaEnabled,
        MfaMethodType method,
        LocalDateTime registeredAt,
        LocalDateTime lastUsedAt,
        boolean exceptionActive,
        LocalDateTime exceptionExpiresAt,
        boolean requiredByPolicy
) {}
