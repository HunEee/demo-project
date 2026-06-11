package com.example.authapp.domain.admin.dto;

public record AdminUserStatusRequest(
        String status,
        String reason
) {
}
