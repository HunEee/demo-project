package com.example.authapp.domain.mfa.dto;

import com.example.authapp.domain.mfa.entity.MfaMethodType;

public record MfaVerifyRequest(
        String challengeId,
        MfaMethodType methodType,
        String code
) {}
