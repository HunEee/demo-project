package com.example.authapp.domain.hr.dto;

public record HrUserMasterRequest(
        String employeeNo,
        String name,
        String email,
        String phone,
        String departmentCode,
        String departmentName,
        String position,
        String employmentType,
        String hrStatus,
        String joinedAt,
        String leftAt
) {
}
