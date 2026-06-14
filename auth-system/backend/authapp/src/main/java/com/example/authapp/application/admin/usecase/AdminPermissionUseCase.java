package com.example.authapp.application.admin.usecase;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.authapp.domain.authorization.dto.AdminPermissionRequest;
import com.example.authapp.domain.authorization.dto.AdminPermissionResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminPermissionUseCase {

    private final AdminPermissionService service;

    public List<AdminPermissionResponse> list() {
        return service.list();
    }

    public AdminPermissionResponse create(AdminPermissionRequest request) {
        return service.create(request);
    }

    public AdminPermissionResponse update(Long id, AdminPermissionRequest request) {
        return service.update(id, request);
    }

    public void delete(Long id) {
        service.delete(id);
    }
}
