package com.example.authapp.application.admin.usecase;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.authapp.domain.authorization.dto.AdminRoleDetailResponse;
import com.example.authapp.domain.authorization.dto.AdminRolePermissionRequest;
import com.example.authapp.domain.authorization.dto.AdminRoleRequest;
import com.example.authapp.domain.authorization.dto.AdminRoleResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminRoleUseCase {

    private final AdminRoleService service;

    public List<AdminRoleResponse> list() {
        return service.list();
    }

    public AdminRoleDetailResponse detail(Long id) {
        return service.detail(id);
    }

    public AdminRoleResponse create(AdminRoleRequest request) {
        return service.create(request);
    }

    public AdminRoleResponse update(Long id, AdminRoleRequest request) {
        return service.update(id, request);
    }

    public void disable(Long id) {
        service.disable(id);
    }

    public void delete(Long id) {
        service.delete(id);
    }

    public AdminRoleDetailResponse assignPermission(Long id, AdminRolePermissionRequest request) {
        return service.assignPermission(id, request);
    }

    public AdminRoleDetailResponse removePermission(Long id, Long permissionId, String reason) {
        return service.removePermission(id, permissionId, reason);
    }
}
