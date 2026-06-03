package com.example.authapp.domain.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.authapp.domain.audit.repository.AuthEventLogRepository;
import com.example.authapp.domain.audit.repository.LoginHistoryRepository;
import com.example.authapp.domain.audit.repository.SecurityIncidentRepository;
import com.example.authapp.domain.jwt.repository.RefreshTokenRepository;
import com.example.authapp.domain.mfa.repository.MfaMethodRepository;
import com.example.authapp.domain.organization.entity.DepartmentEntity;
import com.example.authapp.domain.organization.repository.DepartmentRepository;
import com.example.authapp.domain.organization.repository.GroupUserRepository;
import com.example.authapp.domain.profile.entity.EmploymentType;
import com.example.authapp.domain.profile.entity.UserProfileEntity;
import com.example.authapp.domain.profile.entity.UserProfileStatus;
import com.example.authapp.domain.profile.repository.UserProfileRepository;
import com.example.authapp.domain.risk.repository.RiskRepository;
import com.example.authapp.domain.user.entity.UserEntity;
import com.example.authapp.domain.user.entity.UserRoleType;
import com.example.authapp.domain.authorization.repository.RoleRepository;
import com.example.authapp.domain.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AdminConsoleServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private LoginHistoryRepository loginHistoryRepository;

    @Mock
    private AuthEventLogRepository authEventLogRepository;

    @Mock
    private SecurityIncidentRepository securityIncidentRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private RiskRepository riskRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private MfaMethodRepository mfaMethodRepository;

    @Mock
    private GroupUserRepository groupUserRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private com.example.authapp.domain.audit.repository.AdminActionLogRepository adminActionLogRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void filterOptionsBuildsRoleOptionsFromEnumWithoutQueryingRolesTable() {
        AdminConsoleService service = new AdminConsoleService(
                userRepository,
                loginHistoryRepository,
                authEventLogRepository,
                securityIncidentRepository,
                refreshTokenRepository,
                riskRepository,
                userProfileRepository,
                mfaMethodRepository,
                groupUserRepository,
                roleRepository,
                departmentRepository,
                adminActionLogRepository,
                passwordEncoder
        );

        var options = service.filterOptions();

        assertThat(options.roles())
                .extracting("value")
                .containsExactly(UserRoleType.ROLE_ADMIN.name(), UserRoleType.ROLE_USER.name());
        verifyNoInteractions(roleRepository);
    }

    @Test
    void usersCanBeFilteredByProfileAndMfaFieldsForExternalUserManagement() {
        AdminConsoleService service = new AdminConsoleService(
                userRepository,
                loginHistoryRepository,
                authEventLogRepository,
                securityIncidentRepository,
                refreshTokenRepository,
                riskRepository,
                userProfileRepository,
                mfaMethodRepository,
                groupUserRepository,
                roleRepository,
                departmentRepository,
                adminActionLogRepository,
                passwordEncoder
        );
        UserEntity partner = UserEntity.builder()
                .id(1L)
                .username("partner1")
                .email("partner@example.com")
                .nickname("Partner One")
                .enabled(true)
                .locked(false)
                .social(false)
                .build();
        DepartmentEntity department = DepartmentEntity.builder()
                .id(7L)
                .name("Partners")
                .code("PARTNER")
                .enabled(true)
                .build();
        UserProfileEntity profile = UserProfileEntity.builder()
                .username("partner1")
                .employeeNo("EXT-100")
                .department(department)
                .position("Consultant")
                .employmentType(EmploymentType.EXTERNAL)
                .status(UserProfileStatus.ACTIVE)
                .expiresAt(LocalDateTime.of(2026, 7, 1, 0, 0))
                .build();

        when(userRepository.findAll()).thenReturn(List.of(partner));
        when(userProfileRepository.findByUsername("partner1")).thenReturn(java.util.Optional.of(profile));
        when(mfaMethodRepository.existsByUsernameAndEnabledTrue("partner1")).thenReturn(true);

        var matching = service.users(
                0,
                10,
                "EXT-100",
                "",
                "",
                "username",
                "ASC",
                null,
                "EXTERNAL",
                "2026-07-02",
                true
        );
        var hiddenFromEmployeeFilter = service.users(
                0,
                10,
                "",
                "",
                "",
                "username",
                "ASC",
                null,
                "EMPLOYEE",
                null,
                null
        );

        assertThat(matching.getContent()).extracting("username").containsExactly("partner1");
        assertThat(hiddenFromEmployeeFilter.getContent()).isEmpty();
    }
}
