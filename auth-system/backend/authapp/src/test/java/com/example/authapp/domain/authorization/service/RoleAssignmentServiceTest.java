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

    @Test
    void requiresSensitiveReasonWhenAssigningRoleWithSensitivePermissionToUser() {
        RoleAssignmentService service = new RoleAssignmentService(userRepository, groupRepository, roleRepository, historyRepository);
        UserEntity user = UserEntity.builder().username("operator1").build();
        RoleEntity role = sensitiveRole();

        when(userRepository.findByUsername("operator1")).thenReturn(Optional.of(user));
        when(roleRepository.findByName("ROLE_AUTH_OPERATOR")).thenReturn(Optional.of(role));

        assertThatThrownBy(() -> service.assignUserRole("operator1", new AdminRoleAssignmentRequest(
                null,
                "ROLE_AUTH_OPERATOR",
                "운영자 배정",
                ""
        ))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Sensitive reason is required");
    }

    @Test
    void assignsUserRoleAndRecordsAssignmentHistory() {
        RoleAssignmentService service = new RoleAssignmentService(userRepository, groupRepository, roleRepository, historyRepository);
        UserEntity user = UserEntity.builder().username("operator1").nickname("Operator").build();
        RoleEntity role = sensitiveRole();

        when(userRepository.findByUsername("operator1")).thenReturn(Optional.of(user));
        when(roleRepository.findByName("ROLE_AUTH_OPERATOR")).thenReturn(Optional.of(role));

        service.assignUserRole("operator1", new AdminRoleAssignmentRequest(
                null,
                "ROLE_AUTH_OPERATOR",
                "운영자 배정",
                "토큰 폐기 권한이 필요한 보안 운영 업무"
        ));

        ArgumentCaptor<RoleAssignmentHistoryEntity> historyCaptor = ArgumentCaptor.forClass(RoleAssignmentHistoryEntity.class);
        verify(historyRepository).save(historyCaptor.capture());
        assertThat(user.getRoles()).contains(role);
        assertThat(historyCaptor.getValue().getTargetType()).isEqualTo("USER");
        assertThat(historyCaptor.getValue().getTargetId()).isEqualTo("operator1");
        assertThat(historyCaptor.getValue().getAction()).isEqualTo("ASSIGN");
        assertThat(historyCaptor.getValue().isSensitive()).isTrue();
        assertThat(historyCaptor.getValue().getSensitiveReason()).contains("토큰 폐기");
    }

    @Test
    void assignsGroupRoleThroughGroupRolesJoinTableAndRecordsHistory() {
        RoleAssignmentService service = new RoleAssignmentService(userRepository, groupRepository, roleRepository, historyRepository);
        GroupEntity group = GroupEntity.builder().name("Auth Operators").enabled(true).build();
        RoleEntity role = RoleEntity.builder().name("ROLE_AUTH_VIEWER").enabled(true).build();

        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(roleRepository.findByName("ROLE_AUTH_VIEWER")).thenReturn(Optional.of(role));

        service.assignGroupRole(10L, new AdminRoleAssignmentRequest(
                null,
                "ROLE_AUTH_VIEWER",
                "그룹 역할 배정",
                null
        ));

        ArgumentCaptor<RoleAssignmentHistoryEntity> historyCaptor = ArgumentCaptor.forClass(RoleAssignmentHistoryEntity.class);
        verify(historyRepository).save(historyCaptor.capture());
        assertThat(group.getRoles()).contains(role);
        assertThat(historyCaptor.getValue().getTargetType()).isEqualTo("GROUP");
        assertThat(historyCaptor.getValue().getRoleName()).isEqualTo("ROLE_AUTH_VIEWER");
    }

    private RoleEntity sensitiveRole() {
        RoleEntity role = RoleEntity.builder()
                .name("ROLE_AUTH_OPERATOR")
                .enabled(true)
                .build();
        role.addPermission(PermissionEntity.builder()
                .code("USER_TOKEN_REVOKE")
                .name("토큰 폐기")
                .category("USER")
                .sensitive(true)
                .enabled(true)
                .build());
        return role;
    }
}
