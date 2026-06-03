package com.example.authapp.api.admin;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.authapp.domain.organization.dto.AdminGroupDetailResponse;
import com.example.authapp.domain.organization.dto.AdminGroupMemberRequest;
import com.example.authapp.domain.organization.dto.AdminGroupRequest;
import com.example.authapp.domain.organization.dto.AdminGroupResponse;
import com.example.authapp.domain.organization.dto.AdminGroupRoleRequest;
import com.example.authapp.domain.organization.service.AdminGroupService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/groups")
@RequiredArgsConstructor
public class AdminGroupController {

    private final AdminGroupService adminGroupService;

    @GetMapping
    public List<AdminGroupResponse> groups() {
        return adminGroupService.list();
    }

    @GetMapping("/{id}")
    public AdminGroupDetailResponse detail(@PathVariable Long id) {
        return adminGroupService.detail(id);
    }

    @PostMapping
    public AdminGroupResponse create(@RequestBody AdminGroupRequest request) {
        return adminGroupService.create(request);
    }

    @PatchMapping("/{id}")
    public AdminGroupResponse update(@PathVariable Long id, @RequestBody AdminGroupRequest request) {
        return adminGroupService.update(id, request);
    }

    @PostMapping("/{id}/disable")
    public void disable(
            @PathVariable Long id,
            @RequestParam(name = "reason", required = false) String reason
    ) {
        adminGroupService.disable(id, reason);
    }

    @PostMapping("/{id}/members")
    public AdminGroupDetailResponse addMember(@PathVariable Long id, @RequestBody AdminGroupMemberRequest request) {
        return adminGroupService.addMember(id, request);
    }

    @DeleteMapping("/{id}/members/{username}")
    public AdminGroupDetailResponse removeMember(
            @PathVariable Long id,
            @PathVariable String username,
            @RequestParam(name = "reason", required = false) String reason
    ) {
        return adminGroupService.removeMember(id, username, reason);
    }

    @PostMapping("/{id}/roles")
    public AdminGroupDetailResponse assignRole(@PathVariable Long id, @RequestBody AdminGroupRoleRequest request) {
        return adminGroupService.assignRole(id, request);
    }

    @DeleteMapping("/{id}/roles/{roleId}")
    public AdminGroupDetailResponse removeRole(
            @PathVariable Long id,
            @PathVariable Long roleId,
            @RequestParam(name = "reason", required = false) String reason
    ) {
        return adminGroupService.removeRole(id, roleId, reason);
    }
}
