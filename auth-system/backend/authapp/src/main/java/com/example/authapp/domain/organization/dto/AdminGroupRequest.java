package com.example.authapp.domain.organization.dto;

public record AdminGroupRequest(
        String name,
        String type,
        String ownerUsername,
        String description,
        Boolean enabled,
        String reason
) {
}
