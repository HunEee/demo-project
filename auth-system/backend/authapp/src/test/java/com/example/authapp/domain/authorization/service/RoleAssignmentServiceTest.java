package com.example.authapp.domain.authorization.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import com.example.authapp.domain.authorization.dto.AdminRoleAssignmentRequest;
import com.example.authapp.domain.authorization.entity.PermissionEntity;
import com.example.authapp.domain.authorization.entity.RoleAssignmentHistoryEntity;
import com.example.authapp.domain.authorization.entity.RoleEntity;
import com.example.authapp.domain.authorization.repository.RoleAssignmentHistoryRepository;
import com.example.authapp.domain.authorization.repository.RoleRepository;
import com.example.authapp.domain.organization.entity.GroupEntity;
import com.example.authapp.domain.organization.repository.GroupRepository;
import com.example.authapp.domain.user.entity.UserEntity;
import com.example.authapp.domain.user.repository.UserRepository;
import com.example.authapp.security.rbac.RbacAuthorizationService;

@ExtendWith(MockitoExtension.class)
class RoleAssignmentServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private RoleAssignmentHistoryRepository historyRepository;

    @Mock
    private AdminActionLogService adminActionLogService;

    @Mock
    private RbacAuthorizationService rbacAuthorizationService;

    private RoleAssignmentService service() {
        return new RoleAssignmentService(userRepository, groupRepository, roleRepository, historyRepository, adminActionLogService, rbacAuthorizationService);
    }

    @Test
    void requiresSensitiveReasonWhenAssigningRoleWithSensitivePermissionToUser() {
        RoleAssignmentService service = service();
        UserEntity user = UserEntity.builder().username("operator1").build();
        RoleEntity role = sensitiveRole();

        when(userRepository.findByUsername("operator1")).thenReturn(Optional.of(user));
        when(roleRepository.findByName("ROLE_AUTH_OPERATOR")).thenReturn(Optional.of(role));

        assertThatThrownBy(() -> service.assignUserRole("operator1", new AdminRoleAssignmentRequest(
                null,
                "ROLE_AUTH_OPERATOR",
                "Assign auth operator",
                ""
        ))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Sensitive reason is required");
    }

    @Test
    void assignsUserRoleAndRecordsAssignmentHistoryAndAdminActionLog() {
        RoleAssignmentService service = service();
        UserEntity user = UserEntity.builder().username("operator1").nickname("Operator").build();
        RoleEntity role = sensitiveRole();

        when(userRepository.findByUsername("operator1")).thenReturn(Optional.of(user));
        when(roleRepository.findByName("ROLE_AUTH_OPERATOR")).thenReturn(Optional.of(role));

        service.assignUserRole("operator1", new AdminRoleAssignmentRequest(
                null,
                "ROLE_AUTH_OPERATOR",
                "Assign auth operator",
                "Security operation requires token revoke"
        ));

        ArgumentCaptor<RoleAssignmentHistoryEntity> historyCaptor = ArgumentCaptor.forClass(RoleAssignmentHistoryEntity.class);
        verify(historyRepository).save(historyCaptor.capture());
        assertThat(user.getRoles()).contains(role);
        assertThat(historyCaptor.getValue().getTargetType()).isEqualTo("USER");
        assertThat(historyCaptor.getValue().getTargetId()).isEqualTo("operator1");
        assertThat(historyCaptor.getValue().getAction()).isEqualTo("ASSIGN");
        assertThat(historyCaptor.getValue().isSensitive()).isTrue();
        assertThat(historyCaptor.getValue().getSensitiveReason()).contains("token revoke");

        AdminActionLogRequest log = capturedLog();
        assertThat(log.actionType()).isEqualTo(AdminActionType.ASSIGN_ROLE);
        assertThat(log.targetType()).isEqualTo("USER");
        assertThat(log.targetId()).isEqualTo("operator1");
        assertThat(log.reason()).isEqualTo("Assign auth operator");
        assertThat(log.metadata()).contains("ROLE_AUTH_OPERATOR");
        verify(rbacAuthorizationService).invalidateUserPermissionCache("operator1");
    }

    @Test
    void revokesUserRoleAndRecordsAdminActionLog() {
        RoleAssignmentService service = service();
        UserEntity user = UserEntity.builder().username("operator1").nickname("Operator").build();
        RoleEntity role = RoleEntity.builder().name("ROLE_AUTH_VIEWER").enabled(true).build();
        user.addRole(role);

        when(userRepository.findByUsername("operator1")).thenReturn(Optional.of(user));
        when(roleRepository.findById(7L)).thenReturn(Optional.of(role));

        service.revokeUserRole("operator1", 7L, "Remove viewer role");

        assertThat(user.getRoles()).doesNotContain(role);
        AdminActionLogRequest log = capturedLog();
        assertThat(log.actionType()).isEqualTo(AdminActionType.REMOVE_ROLE);
        assertThat(log.targetType()).isEqualTo("USER");
        assertThat(log.targetId()).isEqualTo("operator1");
        assertThat(log.reason()).isEqualTo("Remove viewer role");
        assertThat(log.metadata()).contains("ROLE_AUTH_VIEWER");
        verify(rbacAuthorizationService).invalidateUserPermissionCache("operator1");
    }

    @Test
    void assignsGroupRoleThroughGroupRolesJoinTableAndRecordsHistoryAndAdminActionLog() {
        RoleAssignmentService service = service();
        GroupEntity group = GroupEntity.builder().name("Auth Operators").enabled(true).build();
        RoleEntity role = RoleEntity.builder().name("ROLE_AUTH_VIEWER").enabled(true).build();

        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(roleRepository.findByName("ROLE_AUTH_VIEWER")).thenReturn(Optional.of(role));

        service.assignGroupRole(10L, new AdminRoleAssignmentRequest(
                null,
                "ROLE_AUTH_VIEWER",
                "Assign group role",
                null
        ));

        ArgumentCaptor<RoleAssignmentHistoryEntity> historyCaptor = ArgumentCaptor.forClass(RoleAssignmentHistoryEntity.class);
        verify(historyRepository).save(historyCaptor.capture());
        assertThat(group.getRoles()).contains(role);
        assertThat(historyCaptor.getValue().getTargetType()).isEqualTo("GROUP");
        assertThat(historyCaptor.getValue().getRoleName()).isEqualTo("ROLE_AUTH_VIEWER");

        AdminActionLogRequest log = capturedLog();
        assertThat(log.actionType()).isEqualTo(AdminActionType.ASSIGN_ROLE);
        assertThat(log.targetType()).isEqualTo("GROUP");
        assertThat(log.targetId()).isEqualTo("10");
        assertThat(log.reason()).isEqualTo("Assign group role");
        verify(rbacAuthorizationService).invalidateAllUserPermissionCache();
    }

    private RoleEntity sensitiveRole() {
        RoleEntity role = RoleEntity.builder()
                .name("ROLE_AUTH_OPERATOR")
                .enabled(true)
                .build();
        role.addPermission(PermissionEntity.builder()
                .code("USER_TOKEN_REVOKE")
                .name("Revoke user token")
                .category("USER")
                .sensitive(true)
                .enabled(true)
                .build());
        return role;
    }

    private AdminActionLogRequest capturedLog() {
        ArgumentCaptor<AdminActionLogRequest> logCaptor = ArgumentCaptor.forClass(AdminActionLogRequest.class);
        verify(adminActionLogService).record(logCaptor.capture());
        return logCaptor.getValue();
    }
}
