package com.example.authapp.domain.mfa.dto;

public record TotpSetupResponse(
        Long methodId,
        String secret,
        String otpAuthUri,
        String qrCodeDataUri
) {}
