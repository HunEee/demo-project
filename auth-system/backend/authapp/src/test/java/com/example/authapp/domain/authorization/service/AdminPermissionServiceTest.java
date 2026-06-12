package com.example.authapp.domain.authorization.service;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.example.authapp.domain.authorization.dto.AdminPermissionRequest;
import com.example.authapp.domain.authorization.entity.PermissionEntity;
import com.example.authapp.domain.authorization.repository.PermissionRepository;
import com.example.authapp.security.rbac.RbacAuthorizationService;

@ExtendWith(MockitoExtension.class)
class AdminPermissionServiceTest {

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private AdminActionLogService adminActionLogService;

    @Mock
    private RbacAuthorizationService rbacAuthorizationService;

    private AdminPermissionService service() {
        return new AdminPermissionService(permissionRepository, adminActionLogService, rbacAuthorizationService);
    }

    @Test
    void createsPermissionAndRecordsAuditLog() {
        AdminPermissionService service = service();
        when(permissionRepository.existsByCode("USER_READ")).thenReturn(false);
        when(permissionRepository.save(any(PermissionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(new AdminPermissionRequest(
                "USER_READ",
                "Read users",
                "USER",
                "Can read users",
                false,
                true,
                "Create user read permission"
        ));

        assertThat(response.code()).isEqualTo("USER_READ");

        AdminActionLogRequest log = capturedLog();
        assertThat(log.actionType()).isEqualTo(AdminActionType.CREATE_PERMISSION);
        assertThat(log.targetType()).isEqualTo("PERMISSION");
        assertThat(log.targetName()).isEqualTo("USER_READ");
        assertThat(log.reason()).isEqualTo("Create user read permission");
        assertThat(log.afterValue()).contains("USER_READ");
        verify(rbacAuthorizationService).invalidateAllUserPermissionCache();
    }

    @Test
    void updatesPermissionAndRecordsAuditLog() {
        AdminPermissionService service = service();
        PermissionEntity permission = PermissionEntity.builder()
                .code("USER_READ")
                .name("Read users")
                .category("USER")
                .description("Can read users")
                .sensitive(false)
                .enabled(true)
                .build();

        when(permissionRepository.findById(1L)).thenReturn(Optional.of(permission));

        var response = service.update(1L, new AdminPermissionRequest(
                "USER_READ",
                "Read user accounts",
                "USER",
                "Can read user accounts",
                false,
                true,
                "Clarify permission label"
        ));

        assertThat(response.name()).isEqualTo("Read user accounts");

        AdminActionLogRequest log = capturedLog();
        assertThat(log.actionType()).isEqualTo(AdminActionType.UPDATE_PERMISSION);
        assertThat(log.targetType()).isEqualTo("PERMISSION");
        assertThat(log.targetName()).isEqualTo("USER_READ");
        assertThat(log.reason()).isEqualTo("Clarify permission label");
        assertThat(log.beforeValue()).contains("Read users");
        assertThat(log.afterValue()).contains("Read user accounts");
        verify(rbacAuthorizationService).invalidateAllUserPermissionCache();
    }

    @Test
    void deletesPermissionAndRecordsAuditLog() {
        AdminPermissionService service = service();
        PermissionEntity permission = PermissionEntity.builder()
                .code("USER_DELETE")
                .name("Delete users")
                .category("USER")
                .description("Can delete users")
                .sensitive(true)
                .enabled(true)
                .build();

        when(permissionRepository.findById(1L)).thenReturn(Optional.of(permission));

        service.delete(1L);

        verify(permissionRepository).delete(permission);
        AdminActionLogRequest log = capturedLog();
        assertThat(log.actionType()).isEqualTo(AdminActionType.DELETE_PERMISSION);
        assertThat(log.targetType()).isEqualTo("PERMISSION");
        assertThat(log.targetName()).isEqualTo("USER_DELETE");
        assertThat(log.beforeValue()).contains("USER_DELETE");
        verify(rbacAuthorizationService).invalidateAllUserPermissionCache();
    }

    private AdminActionLogRequest capturedLog() {
        ArgumentCaptor<AdminActionLogRequest> logCaptor = ArgumentCaptor.forClass(AdminActionLogRequest.class);
        verify(adminActionLogService).record(logCaptor.capture());
        return logCaptor.getValue();
    }
}
