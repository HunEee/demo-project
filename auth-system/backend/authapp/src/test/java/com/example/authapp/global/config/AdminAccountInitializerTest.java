package com.example.authapp.global.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.authapp.domain.authorization.entity.RoleEntity;
import com.example.authapp.domain.user.entity.UserEntity;
import com.example.authapp.domain.authorization.repository.RoleRepository;
import com.example.authapp.domain.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AdminAccountInitializerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void createsDefaultAdminAccountWhenMissing() {
        RoleEntity adminRole = RoleEntity.builder().name("ROLE_ADMIN").build();

        when(userRepository.findByUsername("admin")).thenReturn(Optional.empty());
        when(roleRepository.findByName("ROLE_ADMIN")).thenReturn(Optional.of(adminRole));
        when(passwordEncoder.encode("password")).thenReturn("encoded-password");

        AdminAccountInitializer initializer = new AdminAccountInitializer(
                userRepository,
                roleRepository,
                passwordEncoder
        );

        initializer.run();

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(userCaptor.capture());

        UserEntity admin = userCaptor.getValue();
        assertThat(admin.getUsername()).isEqualTo("admin");
        assertThat(admin.getPassword()).isEqualTo("encoded-password");
        assertThat(admin.getEmail()).isEqualTo("admin@example.com");
        assertThat(admin.getNickname()).isEqualTo("Administrator");
        assertThat(admin.isEnabled()).isTrue();
        assertThat(admin.isLocked()).isFalse();
        assertThat(admin.isSocial()).isFalse();
        assertThat(admin.getRoles()).contains(adminRole);
    }

    @Test
    void doesNotOverwriteExistingAdminAccount() {
        RoleEntity adminRole = RoleEntity.builder().name("ROLE_ADMIN").build();
        UserEntity existingAdmin = UserEntity.builder()
                .username("admin")
                .password("already-encoded")
                .email("admin@example.com")
                .nickname("Administrator")
                .locked(false)
                .enabled(true)
                .social(false)
                .build();
        existingAdmin.addRole(adminRole);

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(existingAdmin));

        AdminAccountInitializer initializer = new AdminAccountInitializer(
                userRepository,
                roleRepository,
                passwordEncoder
        );

        initializer.run();

        verify(userRepository, never()).save(any(UserEntity.class));
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void assignsAdminRoleWhenExistingAdminAccountHasNoRole() {
        RoleEntity adminRole = RoleEntity.builder().name("ROLE_ADMIN").build();
        UserEntity existingAdmin = UserEntity.builder()
                .username("admin")
                .password("already-encoded")
                .email("admin@example.com")
                .nickname("Administrator")
                .locked(false)
                .enabled(true)
                .social(false)
                .build();

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(existingAdmin));
        when(roleRepository.findByName("ROLE_ADMIN")).thenReturn(Optional.of(adminRole));

        AdminAccountInitializer initializer = new AdminAccountInitializer(
                userRepository,
                roleRepository,
                passwordEncoder
        );

        initializer.run();

        assertThat(existingAdmin.getRoles()).contains(adminRole);
        verify(userRepository, never()).save(any(UserEntity.class));
        verify(passwordEncoder, never()).encode(any());
    }
}
