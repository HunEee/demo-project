package com.example.authapp.application.admin.usecase;

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
import com.example.authapp.domain.audit.entity.AdminActionType;
import com.example.authapp.domain.audit.service.AdminActionLogRequest;
import com.example.authapp.domain.audit.service.AdminActionLogService;
import com.example.authapp.domain.organization.repository.GroupRepository;
import com.example.authapp.security.rbac.RbacAuthorizationService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminRoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final GroupRepository groupRepository;
    private final AdminActionLogService adminActionLogService;
    private final RbacAuthorizationService rbacAuthorizationService;

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
        RoleEntity saved = roleRepository.save(role);
        recordRoleAction(saved, AdminActionType.CREATE_ROLE, null, roleSnapshot(saved), request.reason(), null);
        rbacAuthorizationService.invalidateAllUserPermissionCache();
        return AdminRoleResponse.from(saved);
    }

    @Transactional
    public AdminRoleResponse update(Long id, AdminRoleRequest request) {
        RoleEntity role = roleRepository.findById(id).orElseThrow();
        String before = roleSnapshot(role);
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
        recordRoleAction(role, AdminActionType.UPDATE_ROLE, before, roleSnapshot(role), request.reason(), null);
        rbacAuthorizationService.invalidateAllUserPermissionCache();
        return AdminRoleResponse.from(role);
    }

    @Transactional
    public void disable(Long id) {
        RoleEntity role = roleRepository.findById(id).orElseThrow();
        String before = roleSnapshot(role);
        if (role.isSystemRole()) {
            throw new IllegalArgumentException("System role cannot be disabled.");
        }
        role.disable();
        recordRoleAction(role, AdminActionType.DISABLE_ROLE, before, roleSnapshot(role), "Disable role", null);
        rbacAuthorizationService.invalidateAllUserPermissionCache();
    }

    @Transactional
    public void delete(Long id) {
        RoleEntity role = roleRepository.findById(id).orElseThrow();
        String before = roleSnapshot(role);
        if (role.isSystemRole()) {
            throw new IllegalArgumentException("System role cannot be deleted.");
        }
        if (!role.getUsers().isEmpty() || groupRepository.existsByRoles_Id(id) || !role.getPermissions().isEmpty()) {
            throw new IllegalArgumentException("Role is in use. Disable it or remove users, groups, and permissions first.");
        }
        roleRepository.delete(role);
        recordRoleAction(role, AdminActionType.DELETE_ROLE, before, null, "Delete role", null);
        rbacAuthorizationService.invalidateAllUserPermissionCache();
    }

    @Transactional
    public AdminRoleDetailResponse assignPermission(Long roleId, AdminRolePermissionRequest request) {
        RoleEntity role = roleRepository.findById(roleId).orElseThrow();
        PermissionEntity permission = findPermission(request);
        String before = roleSnapshot(role);
        role.addPermission(permission);
        recordRoleAction(
                role,
                AdminActionType.ASSIGN_PERMISSION,
                before,
                roleSnapshot(role),
                request.reason(),
                permissionMetadata(permission)
        );
        rbacAuthorizationService.invalidateAllUserPermissionCache();
        return toDetail(role);
    }

    @Transactional
    public AdminRoleDetailResponse removePermission(Long roleId, Long permissionId, String reason) {
        RoleEntity role = roleRepository.findById(roleId).orElseThrow();
        PermissionEntity permission = permissionRepository.findById(permissionId).orElseThrow();
        String before = roleSnapshot(role);
        role.removePermission(permission);
        recordRoleAction(
                role,
                AdminActionType.REMOVE_PERMISSION,
                before,
                roleSnapshot(role),
                reason,
                permissionMetadata(permission)
        );
        rbacAuthorizationService.invalidateAllUserPermissionCache();
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

    private void recordRoleAction(
            RoleEntity role,
            AdminActionType actionType,
            String beforeValue,
            String afterValue,
            String reason,
            String metadata
    ) {
        adminActionLogService.record(AdminActionLogRequest.builder()
                .targetType("ROLE")
                .targetId(role.getId() == null ? null : String.valueOf(role.getId()))
                .targetName(role.getName())
                .actionType(actionType)
                .reason(blankToNull(reason))
                .beforeValue(beforeValue)
                .afterValue(afterValue)
                .metadata(metadata)
                .build());
    }

    private String roleSnapshot(RoleEntity role) {
        return "{"
                + "\"id\":" + nullableNumber(role.getId()) + ","
                + "\"name\":\"" + safe(role.getName()) + "\","
                + "\"displayName\":\"" + safe(role.getDisplayName()) + "\","
                + "\"enabled\":" + role.isEnabled() + ","
                + "\"systemRole\":" + role.isSystemRole() + ","
                + "\"permissionCount\":" + role.getPermissions().size()
                + "}";
    }

    private String permissionMetadata(PermissionEntity permission) {
        return "{"
                + "\"permissionId\":" + nullableNumber(permission.getId()) + ","
                + "\"permissionCode\":\"" + safe(permission.getCode()) + "\""
                + "}";
    }

    private String nullableNumber(Long value) {
        return value == null ? "null" : String.valueOf(value);
    }

    private String safe(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
