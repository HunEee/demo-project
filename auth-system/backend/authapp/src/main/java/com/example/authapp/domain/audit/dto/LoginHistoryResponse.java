package com.example.authapp.domain.audit.dto;

public record LoginHistoryResponse(
        Long id,
        String username,
        String ipAddress,
        String device
) {}

