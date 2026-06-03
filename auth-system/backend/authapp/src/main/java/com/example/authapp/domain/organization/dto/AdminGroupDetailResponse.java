package com.example.authapp.domain.organization.dto;

import java.util.List;

public record AdminGroupDetailResponse(
        AdminGroupResponse group,
        List<AdminGroupMemberResponse> members,
        List<AdminGroupRoleResponse> roles
) {
}
