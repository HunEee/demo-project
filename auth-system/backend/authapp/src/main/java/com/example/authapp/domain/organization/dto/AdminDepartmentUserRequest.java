package com.example.authapp.domain.organization.dto;

public record AdminDepartmentUserRequest(
        String username,
        String employeeNo,
        String position,
        String employmentType,
        String status,
        String expiresAt,
        String reason
) {
}
