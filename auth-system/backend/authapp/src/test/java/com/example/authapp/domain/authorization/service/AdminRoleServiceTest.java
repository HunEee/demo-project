package com.example.authapp.domain.authorization.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.authapp.domain.audit.entity.AdminActionType;
import com.example.authapp.domain.audit.service.AdminActionLogRequest;
import com.example.authapp.domain.audit.service.AdminActionLogService;
import com.example.authapp.domain.authorization.dto.AdminRolePermissionRequest;
import com.example.authapp.domain.authorization.dto.AdminRoleRequest;
import com.example.authapp.domain.authorization.entity.PermissionEntity;
import com.example.authapp.domain.authorization.entity.RoleEntity;
import com.example.authapp.domain.authorization.repository.PermissionRepository;
import com.example.authapp.domain.authorization.repository.RoleRepository;
import com.example.authapp.domain.organization.repository.GroupRepository;
import com.example.authapp.security.rbac.RbacAuthorizationService;

@ExtendWith(MockitoExtension.class)
class AdminRoleServiceTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private AdminActionLogService adminActionLogService;

    @Mock
    private RbacAuthorizationService rbacAuthorizationService;

    private AdminRoleService service() {
        return new AdminRoleService(roleRepository, permissionRepository, groupRepository, adminActionLogService, rbacAuthorizationService);
    }

    @Test
    void createsRoleWithMetadataAndAuditLog() {
        AdminRoleService service = service();
        when(roleRepository.existsByName("ROLE_AUTH_OPERATOR")).thenReturn(false);
        when(roleRepository.save(any(RoleEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(new AdminRoleRequest(
                "ROLE_AUTH_OPERATOR",
                "Auth operator",
                "Can operate auth functions",
                true,
                false,
                "Create auth operator role"
        ));

        ArgumentCaptor<RoleEntity> roleCaptor = ArgumentCaptor.forClass(RoleEntity.class);
        verify(roleRepository).save(roleCaptor.capture());
        assertThat(response.name()).isEqualTo("ROLE_AUTH_OPERATOR");
        assertThat(roleCaptor.getValue().getDisplayName()).isEqualTo("Auth operator");
        assertThat(roleCaptor.getValue().isEnabled()).isTrue();
        assertThat(roleCaptor.getValue().isSystemRole()).isFalse();

        AdminActionLogRequest log = capturedLog();
        assertThat(log.actionType()).isEqualTo(AdminActionType.CREATE_ROLE);
        assertThat(log.targetType()).isEqualTo("ROLE");
        assertThat(log.targetName()).isEqualTo("ROLE_AUTH_OPERATOR");
        assertThat(log.reason()).isEqualTo("Create auth operator role");
        assertThat(log.afterValue()).contains("ROLE_AUTH_OPERATOR");
        verify(rbacAuthorizationService).invalidateAllUserPermissionCache();
    }

    @Test
    void assignsPermissionToRoleThroughJoinTableRelationshipAndAuditLog() {
        AdminRoleService service = service();
        RoleEntity role = RoleEntity.builder()
                .name("ROLE_AUTH_OPERATOR")
                .displayName("Auth operator")
                .enabled(true)
                .systemRole(false)
                .build();
        PermissionEntity permission = PermissionEntity.builder()
                .code("USER_TOKEN_REVOKE")
                .name("Revoke user token")
                .category("USER")
                .sensitive(true)
                .enabled(true)
                .build();

        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(permissionRepository.findById(2L)).thenReturn(Optional.of(permission));

        var detail = service.assignPermission(1L, new AdminRolePermissionRequest(2L, null, "Add token revoke permission"));

        assertThat(role.getPermissions()).contains(permission);
        assertThat(detail.permissions()).extracting("code").containsExactly("USER_TOKEN_REVOKE");

        AdminActionLogRequest log = capturedLog();
        assertThat(log.actionType()).isEqualTo(AdminActionType.ASSIGN_PERMISSION);
        assertThat(log.targetType()).isEqualTo("ROLE");
        assertThat(log.targetName()).isEqualTo("ROLE_AUTH_OPERATOR");
        assertThat(log.reason()).isEqualTo("Add token revoke permission");
        assertThat(log.metadata()).contains("USER_TOKEN_REVOKE");
        verify(rbacAuthorizationService).invalidateAllUserPermissionCache();
    }

    @Test
    void removesPermissionWithReasonAndAuditLog() {
        AdminRoleService service = service();
        RoleEntity role = RoleEntity.builder()
                .name("ROLE_AUTH_OPERATOR")
                .displayName("Auth operator")
                .enabled(true)
                .systemRole(false)
                .build();
        PermissionEntity permission = PermissionEntity.builder()
                .code("USER_TOKEN_REVOKE")
                .name("Revoke user token")
                .category("USER")
                .sensitive(true)
                .enabled(true)
                .build();
        role.addPermission(permission);

        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(permissionRepository.findById(2L)).thenReturn(Optional.of(permission));

        var detail = service.removePermission(1L, 2L, "Remove excessive permission");

        assertThat(role.getPermissions()).doesNotContain(permission);
        assertThat(detail.permissions()).isEmpty();

        AdminActionLogRequest log = capturedLog();
        assertThat(log.actionType()).isEqualTo(AdminActionType.REMOVE_PERMISSION);
        assertThat(log.targetType()).isEqualTo("ROLE");
        assertThat(log.targetName()).isEqualTo("ROLE_AUTH_OPERATOR");
        assertThat(log.reason()).isEqualTo("Remove excessive permission");
        assertThat(log.metadata()).contains("USER_TOKEN_REVOKE");
        verify(rbacAuthorizationService).invalidateAllUserPermissionCache();
    }

    @Test
    void rejectsDisablingSystemRole() {
        AdminRoleService service = service();
        RoleEntity role = RoleEntity.builder()
                .name("ROLE_ADMIN")
                .displayName("Admin")
                .enabled(true)
                .systemRole(true)
                .build();

        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));

        assertThatThrownBy(() -> service.update(1L, new AdminRoleRequest(
                "ROLE_ADMIN",
                "Admin",
                null,
                false,
                true,
                "disable system role"
        ))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsDeletingSystemRoleByDisableEndpoint() {
        AdminRoleService service = service();
        RoleEntity role = RoleEntity.builder()
                .name("ROLE_ADMIN")
                .displayName("Admin")
                .enabled(true)
                .systemRole(true)
                .build();

        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));

        assertThatThrownBy(() -> service.disable(1L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private AdminActionLogRequest capturedLog() {
        ArgumentCaptor<AdminActionLogRequest> logCaptor = ArgumentCaptor.forClass(AdminActionLogRequest.class);
        verify(adminActionLogService).record(logCaptor.capture());
        return logCaptor.getValue();
    }
}
