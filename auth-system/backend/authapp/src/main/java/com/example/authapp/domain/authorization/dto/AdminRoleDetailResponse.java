package com.example.authapp.domain.authorization.dto;

import java.util.List;

public record AdminRoleDetailResponse(
        AdminRoleResponse role,
        List<AdminPermissionResponse> permissions
) {
}
