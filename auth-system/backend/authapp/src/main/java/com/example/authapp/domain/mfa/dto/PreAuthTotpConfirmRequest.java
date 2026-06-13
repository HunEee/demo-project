package com.example.authapp.domain.mfa.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PreAuthTotpConfirmRequest(
        @NotBlank String challengeId,
        @NotNull Long methodId,
        @NotBlank String code
) {
}
