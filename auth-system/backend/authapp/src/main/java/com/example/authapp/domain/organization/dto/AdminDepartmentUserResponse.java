package com.example.authapp.domain.organization.dto;

import java.time.LocalDateTime;

import com.example.authapp.domain.profile.entity.UserProfileEntity;
import com.example.authapp.domain.user.entity.UserEntity;

public record AdminDepartmentUserResponse(
        String username,
        String name,
        String email,
        String employeeNo,
        String position,
        String status,
        String employmentType,
        LocalDateTime expiresAt
) {
    public static AdminDepartmentUserResponse from(UserProfileEntity profile, UserEntity user) {
        return new AdminDepartmentUserResponse(
                profile.getUsername(),
                user != null ? user.getNickname() : null,
                user != null ? user.getEmail() : null,
                profile.getEmployeeNo(),
                profile.getPosition(),
                profile.getStatus() != null ? profile.getStatus().name() : null,
                profile.getEmploymentType() != null ? profile.getEmploymentType().name() : null,
                profile.getExpiresAt()
        );
    }
}
