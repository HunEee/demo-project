package com.example.authapp.domain.organization.dto;

import java.time.LocalDateTime;

import com.example.authapp.domain.organization.entity.GroupUserEntity;
import com.example.authapp.domain.user.entity.UserEntity;

public record AdminGroupMemberResponse(
        String username,
        String name,
        String email,
        LocalDateTime createdAt
) {
    public static AdminGroupMemberResponse from(GroupUserEntity member, UserEntity user) {
        return new AdminGroupMemberResponse(
                member.getUsername(),
                user != null ? user.getNickname() : null,
                user != null ? user.getEmail() : null,
                member.getCreatedAt()
        );
    }
}
