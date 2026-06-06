package com.example.authapp.domain.hr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.authapp.domain.hr.dto.HrUserMasterRequest;
import com.example.authapp.domain.hr.entity.EmploymentType;
import com.example.authapp.domain.hr.entity.HrUserMasterEntity;
import com.example.authapp.domain.hr.entity.HrUserStatus;
import com.example.authapp.domain.hr.repository.HrUserMasterRepository;

@ExtendWith(MockitoExtension.class)
class HrUserMasterServiceTest {

    @Mock
    private HrUserMasterRepository hrUserMasterRepository;

    @Test
    void createsHrUserMasterAsAccountCandidate() {
        HrUserMasterService service = new HrUserMasterService(hrUserMasterRepository);
        when(hrUserMasterRepository.existsByEmployeeNo("E1001")).thenReturn(false);
        when(hrUserMasterRepository.existsByEmail("lee@example.com")).thenReturn(false);
        when(hrUserMasterRepository.save(any(HrUserMasterEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(new HrUserMasterRequest(
                "E1001",
                "Lee User",
                "lee@example.com",
                "010-0000-0000",
                "AUTH",
                "Authentication Team",
                "Engineer",
                "EMPLOYEE",
                "ACTIVE",
                "2026-01-01",
                null
        ));

        assertThat(response.employeeNo()).isEqualTo("E1001");
        assertThat(response.name()).isEqualTo("Lee User");
        assertThat(response.departmentName()).isEqualTo("Authentication Team");
        assertThat(response.employmentType()).isEqualTo(EmploymentType.EMPLOYEE.name());
        assertThat(response.hrStatus()).isEqualTo(HrUserStatus.ACTIVE.name());
        assertThat(response.accountStatus()).isEqualTo("NOT_CREATED");
    }

    @Test
    void rejectsDuplicateEmployeeNo() {
        HrUserMasterService service = new HrUserMasterService(hrUserMasterRepository);
        when(hrUserMasterRepository.existsByEmployeeNo("E1001")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new HrUserMasterRequest(
                "E1001",
                "Lee User",
                "lee@example.com",
                null,
                null,
                null,
                null,
                "EMPLOYEE",
                "ACTIVE",
                null,
                null
        ))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Employee number already exists");
    }

    @Test
    void checksDuplicateFieldsForHrUserMasterForm() {
        HrUserMasterService service = new HrUserMasterService(hrUserMasterRepository);
        when(hrUserMasterRepository.existsByEmployeeNo("E1001")).thenReturn(true);
        when(hrUserMasterRepository.existsByEmail("lee@example.com")).thenReturn(true);
        when(hrUserMasterRepository.existsByPhone("010-1234-5678")).thenReturn(true);

        assertThat(service.exists("employeeNo", "E1001")).isTrue();
        assertThat(service.exists("email", "lee@example.com")).isTrue();
        assertThat(service.exists("phone", "010-1234-5678")).isTrue();
    }

    @Test
    void deletesHrUserMasterOnlyWhenAccountIsNotCreated() {
        HrUserMasterService service = new HrUserMasterService(hrUserMasterRepository);
        HrUserMasterEntity candidate = HrUserMasterEntity.builder()
                .employeeNo("E1001")
                .name("Lee User")
                .email("lee@example.com")
                .employmentType(EmploymentType.EMPLOYEE)
                .hrStatus(HrUserStatus.ACTIVE)
                .build();

        when(hrUserMasterRepository.findById(1L)).thenReturn(Optional.of(candidate));

        service.delete(1L);

        verify(hrUserMasterRepository).delete(candidate);
    }

    @Test
    void marksHrUserMasterAccountCreatedOnlyOnce() {
        HrUserMasterService service = new HrUserMasterService(hrUserMasterRepository);
        HrUserMasterEntity candidate = HrUserMasterEntity.builder()
                .employeeNo("E1001")
                .name("Lee User")
                .email("lee@example.com")
                .employmentType(EmploymentType.EMPLOYEE)
                .hrStatus(HrUserStatus.ACTIVE)
                .build();

        when(hrUserMasterRepository.findByEmployeeNo("E1001")).thenReturn(Optional.of(candidate));

        var response = service.markAccountCreated("E1001", "lee.user");

        assertThat(response.accountStatus()).isEqualTo("CREATED");
        assertThat(response.accountUsername()).isEqualTo("lee.user");
        assertThatThrownBy(() -> service.markAccountCreated("E1001", "lee.user2"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Account already created");
    }
}
