package com.example.authapp.domain.organization.dto;

public record AdminGroupMemberRequest(
        String username,
        String reason
) {
}
