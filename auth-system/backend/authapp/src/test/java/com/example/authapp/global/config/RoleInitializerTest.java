package com.example.authapp.global.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.authapp.domain.authorization.entity.RoleEntity;
import com.example.authapp.domain.authorization.repository.RoleRepository;
import com.example.authapp.domain.user.entity.UserRoleType;

@ExtendWith(MockitoExtension.class)
class RoleInitializerTest {

    @Mock
    private RoleRepository roleRepository;

    @Test
    void restoresExistingSystemRoleDefaults() {
        RoleEntity adminRole = RoleEntity.builder()
                .name(UserRoleType.ROLE_ADMIN.name())
                .displayName("Admin")
                .description("custom")
                .enabled(false)
                .systemRole(false)
                .build();

        when(roleRepository.findByNameIn(List.of(UserRoleType.ROLE_ADMIN.name(), UserRoleType.ROLE_USER.name())))
                .thenReturn(List.of(adminRole));

        RoleInitializer initializer = new RoleInitializer(roleRepository);

        initializer.init();

        assertThat(adminRole.isEnabled()).isTrue();
        assertThat(adminRole.isSystemRole()).isTrue();
        assertThat(adminRole.getDisplayName()).isEqualTo(UserRoleType.ROLE_ADMIN.getLabel());
        verify(roleRepository).save(adminRole);
    }
}
