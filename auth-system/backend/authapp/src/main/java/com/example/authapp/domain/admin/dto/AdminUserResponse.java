package com.example.authapp.domain.admin.dto;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

import com.example.authapp.domain.user.entity.RoleEntity;
import com.example.authapp.domain.user.entity.UserEntity;

public record AdminUserResponse(
        Long id,
        String username,
        String email,
        String nickname,
        boolean locked,
        boolean enabled,
        boolean deleted,
        boolean social,
        Set<String> roles,
        LocalDateTime createdAt
) {
    public static AdminUserResponse from(UserEntity user) {
        return new AdminUserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getNickname(),
                user.isLocked(),
                user.isEnabled(),
                user.isDeleted(),
                user.isSocial(),
                user.getRoles().stream().map(RoleEntity::getName).collect(Collectors.toSet()),
                user.getCreatedAt()
        );
    }
}
