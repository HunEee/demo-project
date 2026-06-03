package com.example.authapp.domain.admin.dto;

public record AdminUserCreateRequest(
        String username,
        String password,
        String email,
        String name,
        String employeeNo,
        Long departmentId,
        String position,
        String employmentType,
        String status,
        String expiresAt,
        String roleName,
        String reason
) {
}
