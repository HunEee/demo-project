package com.example.authapp.domain.admin.dto;

public record AdminPasswordResetResponse(
        String username,
        String temporaryPassword
) {
}
