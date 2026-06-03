package com.example.authapp.domain.admin.dto;

public record AdminUserUpdateRequest(
        String email,
        String name,
        String employeeNo,
        Long departmentId,
        String position,
        String employmentType,
        String status,
        String expiresAt,
        String reason
) {
}
