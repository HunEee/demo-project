package com.example.authapp.domain.mfa.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.example.authapp.domain.mfa.entity.MfaMethodType;

public record MfaChallengeResult(
        String challengeId,
        LocalDateTime expiresAt,
        boolean registrationRequired,
        List<MfaMethodType> availableMethods
) {}
