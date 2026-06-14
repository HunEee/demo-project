package com.example.authapp.application.admin.usecase;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.authapp.domain.organization.dto.AdminGroupDetailResponse;
import com.example.authapp.domain.organization.dto.AdminGroupMemberRequest;
import com.example.authapp.domain.organization.dto.AdminGroupRequest;
import com.example.authapp.domain.organization.dto.AdminGroupResponse;
import com.example.authapp.domain.organization.dto.AdminGroupRoleRequest;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminGroupUseCase {

    private final AdminGroupService service;

    public List<AdminGroupResponse> list() {
        return service.list();
    }

    public AdminGroupDetailResponse detail(Long id) {
        return service.detail(id);
    }

    public AdminGroupResponse create(AdminGroupRequest request) {
        return service.create(request);
    }

    public AdminGroupResponse update(Long id, AdminGroupRequest request) {
        return service.update(id, request);
    }

    public void disable(Long id, String reason) {
        service.disable(id, reason);
    }

    public AdminGroupDetailResponse addMember(Long id, AdminGroupMemberRequest request) {
        return service.addMember(id, request);
    }

    public AdminGroupDetailResponse removeMember(Long id, String username, String reason) {
        return service.removeMember(id, username, reason);
    }

    public AdminGroupDetailResponse assignRole(Long id, AdminGroupRoleRequest request) {
        return service.assignRole(id, request);
    }

    public AdminGroupDetailResponse removeRole(Long id, Long roleId, String reason) {
        return service.removeRole(id, roleId, reason);
    }
}
