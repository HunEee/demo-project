package com.example.authapp.domain.mfa.dto;

public record TotpConfirmRequest(
        Long methodId,
        String code
) {}
