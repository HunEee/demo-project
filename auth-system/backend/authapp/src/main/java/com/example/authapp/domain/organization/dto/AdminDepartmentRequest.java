package com.example.authapp.domain.organization.dto;

public record AdminDepartmentRequest(
        String name,
        String code,
        Long parentId,
        String managerUsername,
        Boolean enabled,
        Integer displayOrder,
        String reason
) {
}
