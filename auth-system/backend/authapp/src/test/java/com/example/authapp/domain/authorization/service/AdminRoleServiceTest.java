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

import com.example.authapp.domain.authorization.dto.AdminRolePermissionRequest;
import com.example.authapp.domain.authorization.dto.AdminRoleRequest;
import com.example.authapp.domain.authorization.entity.PermissionEntity;
import com.example.authapp.domain.authorization.entity.RoleEntity;
import com.example.authapp.domain.authorization.repository.PermissionRepository;
import com.example.authapp.domain.authorization.repository.RoleRepository;

@ExtendWith(MockitoExtension.class)
class AdminRoleServiceTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @Test
    void createsRoleWithMetadata() {
        AdminRoleService service = new AdminRoleService(roleRepository, permissionRepository);
        when(roleRepository.existsByName("ROLE_AUTH_OPERATOR")).thenReturn(false);
        when(roleRepository.save(any(RoleEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(new AdminRoleRequest(
                "ROLE_AUTH_OPERATOR",
                "운영자",
                "사용자 잠금과 세션 폐기를 수행합니다.",
                true,
                false,
                "운영 역할 생성"
        ));

        ArgumentCaptor<RoleEntity> roleCaptor = ArgumentCaptor.forClass(RoleEntity.class);
        verify(roleRepository).save(roleCaptor.capture());
        assertThat(response.name()).isEqualTo("ROLE_AUTH_OPERATOR");
        assertThat(roleCaptor.getValue().getDisplayName()).isEqualTo("운영자");
        assertThat(roleCaptor.getValue().isEnabled()).isTrue();
        assertThat(roleCaptor.getValue().isSystemRole()).isFalse();
    }

    @Test
    void assignsPermissionToRoleThroughJoinTableRelationship() {
        AdminRoleService service = new AdminRoleService(roleRepository, permissionRepository);
        RoleEntity role = RoleEntity.builder()
                .name("ROLE_AUTH_OPERATOR")
                .displayName("운영자")
                .enabled(true)
                .systemRole(false)
                .build();
        PermissionEntity permission = PermissionEntity.builder()
                .code("USER_TOKEN_REVOKE")
                .name("토큰 폐기")
                .category("USER")
                .sensitive(true)
                .enabled(true)
                .build();

        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(permissionRepository.findById(2L)).thenReturn(Optional.of(permission));

        var detail = service.assignPermission(1L, new AdminRolePermissionRequest(2L, null, "운영 권한 추가"));

        assertThat(role.getPermissions()).contains(permission);
        assertThat(detail.permissions()).extracting("code").containsExactly("USER_TOKEN_REVOKE");
    }

    @Test
    void rejectsDisablingSystemRole() {
        AdminRoleService service = new AdminRoleService(roleRepository, permissionRepository);
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
        AdminRoleService service = new AdminRoleService(roleRepository, permissionRepository);
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
}
