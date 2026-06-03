package com.example.authapp.domain.authorization.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.authapp.domain.authorization.dto.AdminPermissionResponse;
import com.example.authapp.domain.authorization.dto.AdminRoleDetailResponse;
import com.example.authapp.domain.authorization.dto.AdminRolePermissionRequest;
import com.example.authapp.domain.authorization.dto.AdminRoleRequest;
import com.example.authapp.domain.authorization.dto.AdminRoleResponse;
import com.example.authapp.domain.authorization.entity.PermissionEntity;
import com.example.authapp.domain.authorization.entity.RoleEntity;
import com.example.authapp.domain.authorization.repository.PermissionRepository;
import com.example.authapp.domain.authorization.repository.RoleRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminRoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @Transactional(readOnly = true)
    public List<AdminRoleResponse> list() {
        return roleRepository.findAll().stream()
                .sorted(Comparator.comparing(RoleEntity::getName, String.CASE_INSENSITIVE_ORDER))
                .map(AdminRoleResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminRoleDetailResponse detail(Long id) {
        RoleEntity role = roleRepository.findById(id).orElseThrow();
        return toDetail(role);
    }

    @Transactional
    public AdminRoleResponse create(AdminRoleRequest request) {
        requireText(request.name(), "Role name is required.");
        if (roleRepository.existsByName(request.name())) {
            throw new IllegalArgumentException("Role already exists.");
        }
        RoleEntity role = RoleEntity.builder()
                .name(request.name())
                .displayName(blankToNull(request.displayName()))
                .description(blankToNull(request.description()))
                .enabled(request.enabled() == null || request.enabled())
                .systemRole(request.systemRole() != null && request.systemRole())
                .build();
        return AdminRoleResponse.from(roleRepository.save(role));
    }

    @Transactional
    public AdminRoleResponse update(Long id, AdminRoleRequest request) {
        RoleEntity role = roleRepository.findById(id).orElseThrow();
        if (role.isSystemRole() && Boolean.FALSE.equals(request.enabled())) {
            throw new IllegalArgumentException("System role cannot be disabled.");
        }
        if (role.isSystemRole() && Boolean.FALSE.equals(request.systemRole())) {
            throw new IllegalArgumentException("System role flag cannot be removed.");
        }
        role.update(
                defaultText(request.name(), role.getName()),
                request.displayName() == null ? role.getDisplayName() : blankToNull(request.displayName()),
                request.description() == null ? role.getDescription() : blankToNull(request.description()),
                request.enabled() == null ? role.isEnabled() : request.enabled(),
                request.systemRole() == null ? role.isSystemRole() : request.systemRole()
        );
        return AdminRoleResponse.from(role);
    }

    @Transactional
    public void disable(Long id) {
        RoleEntity role = roleRepository.findById(id).orElseThrow();
        if (role.isSystemRole()) {
            throw new IllegalArgumentException("System role cannot be disabled.");
        }
        role.disable();
    }

    @Transactional
    public AdminRoleDetailResponse assignPermission(Long roleId, AdminRolePermissionRequest request) {
        RoleEntity role = roleRepository.findById(roleId).orElseThrow();
        PermissionEntity permission = findPermission(request);
        role.addPermission(permission);
        return toDetail(role);
    }

    @Transactional
    public AdminRoleDetailResponse removePermission(Long roleId, Long permissionId) {
        RoleEntity role = roleRepository.findById(roleId).orElseThrow();
        PermissionEntity permission = permissionRepository.findById(permissionId).orElseThrow();
        role.removePermission(permission);
        return toDetail(role);
    }

    private AdminRoleDetailResponse toDetail(RoleEntity role) {
        List<AdminPermissionResponse> permissions = role.getPermissions().stream()
                .sorted(Comparator.comparing(PermissionEntity::getCode, String.CASE_INSENSITIVE_ORDER))
                .map(AdminPermissionResponse::from)
                .toList();
        return new AdminRoleDetailResponse(AdminRoleResponse.from(role), permissions);
    }

    private PermissionEntity findPermission(AdminRolePermissionRequest request) {
        if (request.permissionId() != null) {
            return permissionRepository.findById(request.permissionId()).orElseThrow();
        }
        requireText(request.permissionCode(), "Permission id or code is required.");
        return permissionRepository.findByCode(request.permissionCode()).orElseThrow();
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(message);
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
