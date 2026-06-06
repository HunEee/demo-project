package com.example.authapp.domain.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.authapp.domain.audit.repository.AuthEventLogRepository;
import com.example.authapp.domain.audit.repository.LoginHistoryRepository;
import com.example.authapp.domain.audit.repository.SecurityIncidentRepository;
import com.example.authapp.domain.authorization.entity.RoleEntity;
import com.example.authapp.domain.authorization.repository.RoleRepository;
import com.example.authapp.domain.hr.entity.EmploymentType;
import com.example.authapp.domain.hr.entity.HrUserMasterEntity;
import com.example.authapp.domain.hr.entity.HrUserStatus;
import com.example.authapp.domain.hr.repository.HrUserMasterRepository;
import com.example.authapp.domain.jwt.repository.RefreshTokenRepository;
import com.example.authapp.domain.mfa.repository.MfaMethodRepository;
import com.example.authapp.domain.organization.repository.GroupUserRepository;
import com.example.authapp.domain.risk.repository.RiskRepository;
import com.example.authapp.domain.user.entity.UserEntity;
import com.example.authapp.domain.user.entity.UserRoleType;
import com.example.authapp.domain.user.repository.UserRepository;

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
    private HrUserMasterRepository hrUserMasterRepository;

    @Mock
    private MfaMethodRepository mfaMethodRepository;

    @Mock
    private GroupUserRepository groupUserRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private com.example.authapp.domain.audit.repository.AdminActionLogRepository adminActionLogRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AdminConsoleService service() {
        return new AdminConsoleService(
                userRepository,
                loginHistoryRepository,
                authEventLogRepository,
                securityIncidentRepository,
                refreshTokenRepository,
                riskRepository,
                hrUserMasterRepository,
                mfaMethodRepository,
                groupUserRepository,
                roleRepository,
                adminActionLogRepository,
                passwordEncoder
        );
    }

    @Test
    void filterOptionsBuildsRoleOptionsFromEnumWithoutQueryingRolesTable() {
        var options = service().filterOptions();

        assertThat(options.roles())
                .extracting("value")
                .containsExactly(UserRoleType.ROLE_ADMIN.name(), UserRoleType.ROLE_USER.name());
        verifyNoInteractions(roleRepository);
    }

    @Test
    void usersCanBeFilteredByHrMasterAndDirectSignupFields() {
        AdminConsoleService service = service();
        UserEntity partner = UserEntity.builder()
                .id(1L)
                .username("partner1")
                .email("partner@example.com")
                .nickname("Partner One")
                .enabled(true)
                .locked(false)
                .social(false)
                .build();
        HrUserMasterEntity hrUser = HrUserMasterEntity.builder()
                .employeeNo("EXT-100")
                .name("Partner One")
                .email("partner@example.com")
                .departmentCode("PARTNER")
                .departmentName("Partners")
                .position("Consultant")
                .employmentType(EmploymentType.EXTERNAL)
                .hrStatus(HrUserStatus.ACTIVE)
                .accountUsername("partner1")
                .build();

        when(userRepository.findAll()).thenReturn(List.of(partner));
        when(hrUserMasterRepository.findByAccountUsername("partner1")).thenReturn(Optional.of(hrUser));
        when(mfaMethodRepository.existsByUsernameAndEnabledTrue("partner1")).thenReturn(true);

        var matching = service.users(
                0,
                10,
                "EXT-100",
                "",
                "",
                "username",
                "ASC",
                "PARTNER",
                null,
                null,
                true
        );
        var hiddenFromDirectSignupFilter = service.users(
                0,
                10,
                "",
                "",
                "",
                "username",
                "ASC",
                null,
                true,
                null,
                null
        );

        assertThat(matching.getContent()).extracting("username").containsExactly("partner1");
        assertThat(hiddenFromDirectSignupFilter.getContent()).isEmpty();
    }

    @Test
    void createsAccountFromHrUserMasterAndMarksCandidateCreated() {
        AdminConsoleService service = service();
        HrUserMasterEntity hrUser = HrUserMasterEntity.builder()
                .employeeNo("E1001")
                .name("Lee User")
                .email("lee@example.com")
                .departmentCode("AUTH")
                .departmentName("Authentication Team")
                .position("Engineer")
                .employmentType(EmploymentType.EMPLOYEE)
                .hrStatus(HrUserStatus.ACTIVE)
                .build();
        RoleEntity role = RoleEntity.builder()
                .name("ROLE_USER")
                .enabled(true)
                .build();

        when(hrUserMasterRepository.findByEmployeeNo("E1001")).thenReturn(Optional.of(hrUser));
        when(userRepository.existsByUsername("lee.user")).thenReturn(false);
        when(userRepository.existsByEmail("lee@example.com")).thenReturn(false);
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(role));
        when(passwordEncoder.encode("secret")).thenReturn("encoded-secret");
        when(userRepository.save(org.mockito.ArgumentMatchers.any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.createUser(new com.example.authapp.domain.admin.dto.AdminUserCreateRequest(
                "E1001",
                "lee.user",
                "secret",
                "ROLE_USER",
                "HR onboarding"
        ));

        assertThat(response.username()).isEqualTo("lee.user");
        assertThat(response.name()).isEqualTo("Lee User");
        assertThat(response.employeeNo()).isEqualTo("E1001");
        assertThat(response.department()).isEqualTo("Authentication Team");
        assertThat(hrUser.getAccountUsername()).isEqualTo("lee.user");
    }

    @Test
    void checksUsernameDuplicateForAccountCreationForm() {
        when(userRepository.existsByUsername("lee.user")).thenReturn(true);

        assertThat(service().usernameExists("lee.user")).isTrue();
    }
}
