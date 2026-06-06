package com.example.authapp.domain.admin.dto;

public record AdminUserCreateRequest(
        String employeeNo,
        String username,
        String password,
        String roleName,
        String reason
) {
}
