package com.example.authapp.application.admin.usecase;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.authapp.domain.authorization.dto.AdminRoleAssignmentHistoryResponse;
import com.example.authapp.domain.authorization.dto.AdminRoleAssignmentRequest;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminRoleAssignmentUseCase {

    private final RoleAssignmentService service;

    public List<AdminRoleAssignmentHistoryResponse> history() {
        return service.history();
    }

    public void assignUserRole(String username, AdminRoleAssignmentRequest request) {
        service.assignUserRole(username, request);
    }

    public void revokeUserRole(String username, Long roleId, String reason) {
        service.revokeUserRole(username, roleId, reason);
    }
}
