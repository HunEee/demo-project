package com.example.authapp.application.admin.usecase;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.authapp.domain.authorization.dto.AdminApiPermissionRuleRequest;
import com.example.authapp.domain.authorization.dto.AdminApiPermissionRuleResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminApiPermissionRuleUseCase {

    private final AdminApiPermissionRuleService service;

    public List<AdminApiPermissionRuleResponse> list() {
        return service.list();
    }

    public AdminApiPermissionRuleResponse create(AdminApiPermissionRuleRequest request) {
        return service.create(request);
    }

    public AdminApiPermissionRuleResponse update(Long id, AdminApiPermissionRuleRequest request) {
        return service.update(id, request);
    }

    public void delete(Long id) {
        service.delete(id);
    }
}
