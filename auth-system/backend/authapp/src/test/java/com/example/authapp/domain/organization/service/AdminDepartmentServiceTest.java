package com.example.authapp.domain.organization.service;

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

import com.example.authapp.domain.audit.entity.AdminActionLogEntity;
import com.example.authapp.domain.audit.entity.AdminActionType;
import com.example.authapp.domain.audit.repository.AdminActionLogRepository;
import com.example.authapp.domain.organization.dto.AdminDepartmentRequest;
import com.example.authapp.domain.organization.dto.AdminDepartmentUserRequest;
import com.example.authapp.domain.organization.entity.DepartmentEntity;
import com.example.authapp.domain.organization.repository.DepartmentRepository;
import com.example.authapp.domain.profile.entity.EmploymentType;
import com.example.authapp.domain.profile.entity.UserProfileEntity;
import com.example.authapp.domain.profile.entity.UserProfileStatus;
import com.example.authapp.domain.profile.repository.UserProfileRepository;
import com.example.authapp.domain.user.entity.UserEntity;
import com.example.authapp.domain.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AdminDepartmentServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AdminActionLogRepository adminActionLogRepository;

    @Test
    void createsDepartmentAndRecordsGeneralizedAuditTarget() {
        AdminDepartmentService service = new AdminDepartmentService(
                departmentRepository,
                userProfileRepository,
                userRepository,
                adminActionLogRepository
        );
        when(departmentRepository.existsByCode("ENG")).thenReturn(false);
        when(userRepository.findByUsername("lead1"))
                .thenReturn(Optional.of(UserEntity.builder().username("lead1").build()));
        when(departmentRepository.save(any(DepartmentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(new AdminDepartmentRequest(
                "Engineering",
                "ENG",
                null,
                "lead1",
                true,
                10,
                "initial organization setup"
        ));

        ArgumentCaptor<AdminActionLogEntity> logCaptor = ArgumentCaptor.forClass(AdminActionLogEntity.class);
        verify(adminActionLogRepository).save(logCaptor.capture());
        assertThat(response.code()).isEqualTo("ENG");
        assertThat(response.managerUsername()).isEqualTo("lead1");
        assertThat(logCaptor.getValue().getTargetType()).isEqualTo("DEPARTMENT");
        assertThat(logCaptor.getValue().getTargetName()).isEqualTo("Engineering");
        assertThat(logCaptor.getValue().getReason()).isEqualTo("initial organization setup");
    }

    @Test
    void assignsExistingUserToDepartmentAndRecordsDepartmentChangeAudit() {
        AdminDepartmentService service = new AdminDepartmentService(
                departmentRepository,
                userProfileRepository,
                userRepository,
                adminActionLogRepository
        );
        DepartmentEntity department = DepartmentEntity.builder()
                .name("Engineering")
                .code("ENG")
                .enabled(true)
                .displayOrder(10)
                .build();
        UserEntity user = UserEntity.builder()
                .username("user1")
                .email("user1@example.com")
                .nickname("User One")
                .build();

        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(user));
        when(userProfileRepository.findByUsername("user1")).thenReturn(Optional.empty());
        when(userProfileRepository.save(any(UserProfileEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.assignUser(1L, new AdminDepartmentUserRequest(
                "user1",
                "E001",
                "Manager",
                "EMPLOYEE",
                "ACTIVE",
                null,
                "department assignment"
        ));

        ArgumentCaptor<UserProfileEntity> profileCaptor = ArgumentCaptor.forClass(UserProfileEntity.class);
        ArgumentCaptor<AdminActionLogEntity> logCaptor = ArgumentCaptor.forClass(AdminActionLogEntity.class);
        verify(userProfileRepository).save(profileCaptor.capture());
        verify(adminActionLogRepository).save(logCaptor.capture());

        assertThat(response.username()).isEqualTo("user1");
        assertThat(response.employeeNo()).isEqualTo("E001");
        assertThat(profileCaptor.getValue().getDepartment()).isEqualTo(department);
        assertThat(profileCaptor.getValue().getEmploymentType()).isEqualTo(EmploymentType.EMPLOYEE);
        assertThat(profileCaptor.getValue().getStatus()).isEqualTo(UserProfileStatus.ACTIVE);
        assertThat(logCaptor.getValue().getActionType()).isEqualTo(AdminActionType.DEPARTMENT_CHANGE);
        assertThat(logCaptor.getValue().getTargetType()).isEqualTo("USER");
        assertThat(logCaptor.getValue().getTargetUsername()).isEqualTo("user1");
        assertThat(logCaptor.getValue().getReason()).isEqualTo("department assignment");
    }

    @Test
    void rejectsMissingUserWhenAssigningDepartment() {
        AdminDepartmentService service = new AdminDepartmentService(
                departmentRepository,
                userProfileRepository,
                userRepository,
                adminActionLogRepository
        );
        DepartmentEntity department = DepartmentEntity.builder()
                .name("Engineering")
                .code("ENG")
                .enabled(true)
                .displayOrder(10)
                .build();

        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assignUser(1L, new AdminDepartmentUserRequest(
                "missing",
                null,
                null,
                "EMPLOYEE",
                "ACTIVE",
                null,
                "department assignment"
        ))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");
    }
}
