package com.example.authapp.domain.organization.dto;

import java.time.LocalDateTime;

import com.example.authapp.domain.organization.entity.GroupEntity;

public record AdminGroupResponse(
        Long id,
        String name,
        String type,
        String ownerUsername,
        String description,
        boolean enabled,
        long userCount,
        long roleCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AdminGroupResponse from(GroupEntity group, long userCount, long roleCount) {
        return new AdminGroupResponse(
                group.getId(),
                group.getName(),
                group.getType(),
                group.getOwnerUsername(),
                group.getDescription(),
                group.isEnabled(),
                userCount,
                roleCount,
                group.getCreatedAt(),
                group.getUpdatedAt()
        );
    }
}
