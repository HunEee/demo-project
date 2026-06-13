package com.example.authapp.domain.mfa.dto;

import jakarta.validation.constraints.NotBlank;

public record MfaMethodDeleteRequest(@NotBlank String code) {
}
