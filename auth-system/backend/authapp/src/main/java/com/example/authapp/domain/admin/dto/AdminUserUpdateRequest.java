package com.example.authapp.domain.admin.dto;

public record AdminUserUpdateRequest(
        String email,
        String name,
        Boolean locked,
        Boolean enabled,
        String reason
) {
}
