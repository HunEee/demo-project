package com.example.authapp.domain.mfa.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.authapp.domain.admin.AdminSettingsStore;
import com.example.authapp.domain.audit.entity.AdminActionLogEntity;
import com.example.authapp.domain.audit.entity.AdminActionType;
import com.example.authapp.domain.audit.repository.AdminActionLogRepository;
import com.example.authapp.domain.mfa.dto.MfaExceptionRequest;
import com.example.authapp.domain.mfa.entity.MfaExceptionEntity;
import com.example.authapp.domain.mfa.entity.MfaMethodEntity;
import com.example.authapp.domain.mfa.entity.MfaMethodType;
import com.example.authapp.domain.mfa.entity.MfaPolicy;
import com.example.authapp.domain.mfa.repository.MfaExceptionRepository;
import com.example.authapp.domain.mfa.repository.MfaMethodRepository;
import com.example.authapp.domain.authorization.entity.RoleEntity;
import com.example.authapp.domain.user.entity.UserEntity;
import com.example.authapp.domain.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AdminMfaServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private MfaMethodRepository mfaMethodRepository;

    @Mock
    private MfaExceptionRepository mfaExceptionRepository;

    @Mock
    private MfaService mfaService;

    @Mock
    private AdminSettingsStore adminSettingsStore;

    @Mock
    private AdminActionLogRepository adminActionLogRepository;

    private AdminMfaService service() {
        return new AdminMfaService(
                userRepository,
                mfaMethodRepository,
                mfaExceptionRepository,
                mfaService,
                adminSettingsStore,
                adminActionLogRepository
        );
    }

    @Test
    void usersShowsMfaAndExceptionState() {
        UserEntity user = UserEntity.builder().username("user1").email("u@example.com").enabled(true).locked(false).build();
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(3);

        when(userRepository.findAll()).thenReturn(List.of(user));
        when(adminSettingsStore.mfaPolicy()).thenReturn(MfaPolicy.OPTIONAL);
        when(mfaMethodRepository.findByUsernameInAndEnabledTrue(List.of("user1")))
                .thenReturn(List.of(MfaMethodEntity.builder()
                        .username("user1")
                        .type(MfaMethodType.TOTP)
                        .enabled(true)
                        .build()));
        when(mfaExceptionRepository.findActiveByUsernameIn(List.of("user1")))
                .thenReturn(List.of(MfaExceptionEntity.builder()
                        .username("user1")
                        .reason("temporary")
                        .expiresAt(expiresAt)
                        .createdBy("admin")
                        .build()));

        var users = service().users();

        assertThat(users).hasSize(1);
        assertThat(users.get(0).username()).isEqualTo("user1");
        assertThat(users.get(0).mfaEnabled()).isTrue();
        assertThat(users.get(0).exceptionActive()).isTrue();
        assertThat(users.get(0).exceptionExpiresAt()).isEqualTo(expiresAt);
    }

    @Test
    void resetDelegatesToMfaServiceAndWritesAuditLog() {
        service().reset("user1", "lost phone");

        verify(mfaService).resetUserMfa("user1");
        verify(adminActionLogRepository).save(any(AdminActionLogEntity.class));
    }

    @Test
    void policyUpdateStoresPolicyAndWritesAuditLog() {
        when(adminSettingsStore.updateMfaPolicy(MfaPolicy.REQUIRED_FOR_ADMIN)).thenReturn(MfaPolicy.REQUIRED_FOR_ADMIN);
        when(userRepository.findAll()).thenReturn(List.of(adminUser()));
        when(mfaMethodRepository.existsByUsernameAndEnabledTrue("admin")).thenReturn(true);

        var response = service().updatePolicy(MfaPolicy.REQUIRED_FOR_ADMIN);

        assertThat(response.policy()).isEqualTo(MfaPolicy.REQUIRED_FOR_ADMIN);
        verify(adminActionLogRepository).save(any(AdminActionLogEntity.class));
    }

    @Test
    void adminRequiredPolicyIsRejectedWhenNoAdminHasRegisteredMfa() {
        when(userRepository.findAll()).thenReturn(List.of(adminUser()));
        when(mfaMethodRepository.existsByUsernameAndEnabledTrue("admin")).thenReturn(false);

        assertThatThrownBy(() -> service().updatePolicy(MfaPolicy.REQUIRED_FOR_ADMIN))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("admin");
    }

    @Test
    void exceptionRequiresFutureExpiry() {
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(1);

        service().createException("user1", new MfaExceptionRequest("temporary", expiresAt));

        verify(mfaExceptionRepository).save(any(MfaExceptionEntity.class));
        verify(adminActionLogRepository).save(any(AdminActionLogEntity.class));
    }

    @Test
    void createExceptionRevokesExistingActiveExceptionBeforeSavingNewOne() {
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(1);
        MfaExceptionEntity existing = MfaExceptionEntity.builder()
                .username("user1")
                .reason("old")
                .expiresAt(LocalDateTime.now().plusHours(1))
                .createdBy("admin")
                .build();
        when(mfaExceptionRepository.findActiveByUsername("user1")).thenReturn(Optional.of(existing));

        service().createException("user1", new MfaExceptionRequest("temporary", expiresAt));

        assertThat(existing.getRevokedAt()).isNotNull();
        assertThat(existing.getRevokedBy()).isEqualTo("UNKNOWN");
        verify(mfaExceptionRepository).save(any(MfaExceptionEntity.class));
    }

    private UserEntity adminUser() {
        RoleEntity adminRole = RoleEntity.builder().name("ROLE_ADMIN").enabled(true).build();
        UserEntity user = UserEntity.builder()
                .username("admin")
                .email("admin@example.com")
                .enabled(true)
                .locked(false)
                .build();
        user.addRole(adminRole);
        return user;
    }
}
