package com.example.authapp.domain.mfa.dto;

import java.time.LocalDateTime;

public record PreAuthTotpSetupResponse(
        String challengeId,
        Long methodId,
        String secret,
        String otpAuthUri,
        String qrCodeDataUri,
        LocalDateTime expiresAt
) {
}
