package com.example.authapp.domain.mfa.dto;

import java.time.LocalDateTime;

public record MfaExceptionRequest(
        String reason,
        LocalDateTime expiresAt
) {}
