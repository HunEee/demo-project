package com.example.authapp.domain.admin.dto;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

import com.example.authapp.domain.profile.entity.UserProfileEntity;
import com.example.authapp.domain.profile.entity.UserProfileStatus;
import com.example.authapp.domain.user.entity.RoleEntity;
import com.example.authapp.domain.user.entity.SocialProviderType;
import com.example.authapp.domain.user.entity.UserEntity;

public record AdminUserResponse(
        Long id,
        String username,
        String name,
        String email,
        String nickname,
        String employeeNo,
        String department,
        String position,
        String employmentType,
        String status,
        String userType,
        String authMethod,
        boolean mfaEnabled,
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
            UserProfileEntity profile,
            boolean mfaEnabled,
            LocalDateTime latestSuccessfulLoginAt
    ) {
        String derivedStatus = deriveStatus(user, profile);
        SocialProviderType providerType = user.getSocialProviderType();
        String authMethod = user.isSocial()
                ? providerType != null ? providerType.name() : "SOCIAL"
                : "PASSWORD";
        LocalDateTime lastLoginAt = profile != null && profile.getLastLoginAt() != null
                ? profile.getLastLoginAt()
                : latestSuccessfulLoginAt;

        return new AdminUserResponse(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getEmail(),
                user.getNickname(),
                profile != null ? profile.getEmployeeNo() : null,
                profile != null && profile.getDepartment() != null ? profile.getDepartment().getName() : null,
                profile != null ? profile.getPosition() : null,
                profile != null && profile.getEmploymentType() != null ? profile.getEmploymentType().name() : "UNKNOWN",
                derivedStatus,
                user.isSocial() ? "SOCIAL" : "INTERNAL",
                authMethod,
                mfaEnabled,
                lastLoginAt,
                user.isLocked(),
                user.isEnabled(),
                user.isDeleted(),
                user.isSocial(),
                user.getRoles().stream().map(RoleEntity::getName).collect(Collectors.toSet()),
                user.getCreatedAt()
        );
    }

    private static String deriveStatus(UserEntity user, UserProfileEntity profile) {
        if (user.isDeleted()) return UserProfileStatus.DELETED.name();
        if (user.isLocked()) return UserProfileStatus.LOCKED.name();
        if (!user.isEnabled()) return UserProfileStatus.DISABLED.name();
        if (profile != null && profile.getStatus() != null) return profile.getStatus().name();
        return UserProfileStatus.ACTIVE.name();
    }
}
