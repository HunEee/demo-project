package com.example.authapp.domain.hr.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.example.authapp.domain.hr.entity.HrUserMasterEntity;

public record HrUserMasterResponse(
        Long id,
        String employeeNo,
        String name,
        String email,
        String phone,
        String departmentCode,
        String departmentName,
        String position,
        String employmentType,
        String hrStatus,
        String accountStatus,
        String accountUsername,
        LocalDate joinedAt,
        LocalDate leftAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static HrUserMasterResponse from(HrUserMasterEntity entity) {
        return new HrUserMasterResponse(
                entity.getId(),
                entity.getEmployeeNo(),
                entity.getName(),
                entity.getEmail(),
                entity.getPhone(),
                entity.getDepartmentCode(),
                entity.getDepartmentName(),
                entity.getPosition(),
                entity.getEmploymentType() != null ? entity.getEmploymentType().name() : null,
                entity.getHrStatus() != null ? entity.getHrStatus().name() : null,
                entity.getAccountStatus() != null ? entity.getAccountStatus().name() : null,
                entity.getAccountUsername(),
                entity.getJoinedAt(),
                entity.getLeftAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
