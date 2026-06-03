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

import com.example.authapp.domain.authorization.dto.AdminRoleDetailResponse;
import com.example.authapp.domain.authorization.dto.AdminRolePermissionRequest;
import com.example.authapp.domain.authorization.dto.AdminRoleRequest;
import com.example.authapp.domain.authorization.dto.AdminRoleResponse;
import com.example.authapp.domain.authorization.service.AdminRoleService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/roles")
@RequiredArgsConstructor
public class AdminRoleController {

    private final AdminRoleService adminRoleService;

    @GetMapping
    public List<AdminRoleResponse> roles() {
        return adminRoleService.list();
    }

    @GetMapping("/{id}")
    public AdminRoleDetailResponse detail(@PathVariable Long id) {
        return adminRoleService.detail(id);
    }

    @PostMapping
    public AdminRoleResponse create(@RequestBody AdminRoleRequest request) {
        return adminRoleService.create(request);
    }

    @PatchMapping("/{id}")
    public AdminRoleResponse update(@PathVariable Long id, @RequestBody AdminRoleRequest request) {
        return adminRoleService.update(id, request);
    }

    @PostMapping("/{id}/disable")
    public void disable(@PathVariable Long id) {
        adminRoleService.disable(id);
    }

    @PostMapping("/{id}/permissions")
    public AdminRoleDetailResponse assignPermission(@PathVariable Long id, @RequestBody AdminRolePermissionRequest request) {
        return adminRoleService.assignPermission(id, request);
    }

    @DeleteMapping("/{id}/permissions/{permissionId}")
    public AdminRoleDetailResponse removePermission(
            @PathVariable Long id,
            @PathVariable Long permissionId,
            @RequestParam(name = "reason", required = false) String reason
    ) {
        return adminRoleService.removePermission(id, permissionId);
    }
}
