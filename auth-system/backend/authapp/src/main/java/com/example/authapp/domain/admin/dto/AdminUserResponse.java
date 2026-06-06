package com.example.authapp.domain.admin.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

import com.example.authapp.domain.authorization.entity.RoleEntity;
import com.example.authapp.domain.hr.entity.HrUserMasterEntity;
import com.example.authapp.domain.user.entity.SocialProviderType;
import com.example.authapp.domain.user.entity.UserEntity;

public record AdminUserResponse(
        Long id,
        String username,
        String name,
        String email,
        String nickname,
        String employeeNo,
        String departmentCode,
        String department,
        String position,
        String employmentType,
        String status,
        String userType,
        String authMethod,
        boolean mfaEnabled,
        LocalDate joinedAt,
        LocalDate leftAt,
        LocalDateTime lastLoginAt,
        boolean locked,
        boolean enabled,
        boolean deleted,
        boolean social,
        Set<String> roles,
        LocalDateTime createdAt
) {
    public static AdminUserResponse from(UserEntity user) {
        return from(user, null, false, null);
    }

    public static AdminUserResponse from(
            UserEntity user,
            HrUserMasterEntity hrUser,
            boolean mfaEnabled,
            LocalDateTime latestSuccessfulLoginAt
    ) {
        SocialProviderType providerType = user.getSocialProviderType();
        String authMethod = user.isSocial()
                ? providerType != null ? providerType.name() : "SOCIAL"
                : "PASSWORD";

        return new AdminUserResponse(
                user.getId(),
                user.getUsername(),
                hrUser != null ? hrUser.getName() : user.getNickname(),
                user.getEmail(),
                user.getNickname(),
                hrUser != null ? hrUser.getEmployeeNo() : null,
                hrUser != null ? hrUser.getDepartmentCode() : null,
                hrUser != null ? hrUser.getDepartmentName() : null,
                hrUser != null ? hrUser.getPosition() : null,
                hrUser != null && hrUser.getEmploymentType() != null ? hrUser.getEmploymentType().name() : "UNKNOWN",
                deriveStatus(user, hrUser),
                user.isSocial() ? "SOCIAL" : "INTERNAL",
                authMethod,
                mfaEnabled,
                hrUser != null ? hrUser.getJoinedAt() : null,
                hrUser != null ? hrUser.getLeftAt() : null,
                latestSuccessfulLoginAt,
                user.isLocked(),
                user.isEnabled(),
                user.isDeleted(),
                user.isSocial(),
                user.getRoles().stream().map(RoleEntity::getName).collect(Collectors.toSet()),
                user.getCreatedAt()
        );
    }

    private static String deriveStatus(UserEntity user, HrUserMasterEntity hrUser) {
        if (user.isDeleted()) return "DELETED";
        if (user.isLocked()) return "LOCKED";
        if (!user.isEnabled()) return "DISABLED";
        if (hrUser != null && hrUser.getHrStatus() != null) return hrUser.getHrStatus().name();
        return "ACTIVE";
    }
}
