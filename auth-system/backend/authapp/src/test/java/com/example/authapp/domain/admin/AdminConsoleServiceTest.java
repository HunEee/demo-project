package com.example.authapp.domain.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.authapp.domain.audit.repository.AuthEventLogRepository;
import com.example.authapp.domain.audit.repository.LoginHistoryRepository;
import com.example.authapp.domain.audit.repository.SecurityIncidentRepository;
import com.example.authapp.domain.jwt.repository.RefreshTokenRepository;
import com.example.authapp.domain.mfa.repository.MfaMethodRepository;
import com.example.authapp.domain.organization.repository.GroupUserRepository;
import com.example.authapp.domain.profile.repository.UserProfileRepository;
import com.example.authapp.domain.risk.repository.RiskRepository;
import com.example.authapp.domain.user.entity.UserRoleType;
import com.example.authapp.domain.user.repository.RoleRepository;
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
                adminActionLogRepository,
                passwordEncoder
        );

        var options = service.filterOptions();

        assertThat(options.roles())
                .extracting("value")
                .containsExactly(UserRoleType.ROLE_ADMIN.name(), UserRoleType.ROLE_USER.name());
        verifyNoInteractions(roleRepository);
    }
}
