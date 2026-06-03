package com.example.authapp.domain.organization.service;

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

import com.example.authapp.domain.audit.entity.AdminActionLogEntity;
import com.example.authapp.domain.audit.repository.AdminActionLogRepository;
import com.example.authapp.domain.authorization.service.RoleAssignmentService;
import com.example.authapp.domain.organization.dto.AdminGroupRequest;
import com.example.authapp.domain.organization.entity.GroupEntity;
import com.example.authapp.domain.organization.repository.GroupRepository;
import com.example.authapp.domain.organization.repository.GroupUserRepository;
import com.example.authapp.domain.user.entity.UserEntity;
import com.example.authapp.domain.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AdminGroupServiceTest {

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private GroupUserRepository groupUserRepository;

    @Mock
    private RoleAssignmentService roleAssignmentService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AdminActionLogRepository adminActionLogRepository;

    @Test
    void createsStaticGroupWithOwnerAndAuditTarget() {
        AdminGroupService service = new AdminGroupService(
                groupRepository,
                groupUserRepository,
                userRepository,
                roleAssignmentService,
                adminActionLogRepository
        );
        when(groupRepository.existsByName("Finance Approvers")).thenReturn(false);
        when(userRepository.findByUsername("owner1"))
                .thenReturn(Optional.of(UserEntity.builder().username("owner1").build()));
        when(groupRepository.save(any(GroupEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(new AdminGroupRequest(
                "Finance Approvers",
                "STATIC",
                "owner1",
                "Approves finance application access",
                true,
                "new access group"
        ));

        ArgumentCaptor<AdminActionLogEntity> logCaptor = ArgumentCaptor.forClass(AdminActionLogEntity.class);
        verify(adminActionLogRepository).save(logCaptor.capture());
        assertThat(response.name()).isEqualTo("Finance Approvers");
        assertThat(response.type()).isEqualTo("STATIC");
        assertThat(response.ownerUsername()).isEqualTo("owner1");
        assertThat(logCaptor.getValue().getTargetType()).isEqualTo("GROUP");
        assertThat(logCaptor.getValue().getTargetName()).isEqualTo("Finance Approvers");
    }
}
