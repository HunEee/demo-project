package com.example.authapp.domain.hr.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "HR_USER_MASTER",
        indexes = {
                @Index(name = "idx_hr_user_master_employee_no", columnList = "employee_no", unique = true),
                @Index(name = "idx_hr_user_master_email", columnList = "email", unique = true),
                @Index(name = "idx_hr_user_master_account_username", columnList = "account_username", unique = true),
                @Index(name = "idx_hr_user_master_department_code", columnList = "department_code"),
                @Index(name = "idx_hr_user_master_account_status", columnList = "account_status")
        }
)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HrUserMasterEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_no", nullable = false, unique = true, length = 50)
    private String employeeNo;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(length = 30)
    private String phone;

    @Column(name = "department_code", length = 50)
    private String departmentCode;

    @Column(name = "department_name", length = 100)
    private String departmentName;

    @Column(length = 100)
    private String position;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type", nullable = false, length = 30)
    private EmploymentType employmentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "hr_status", nullable = false, length = 30)
    private HrUserStatus hrStatus;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false, length = 30)
    private HrAccountStatus accountStatus = HrAccountStatus.NOT_CREATED;

    @Column(name = "account_username", unique = true, length = 100)
    private String accountUsername;

    @Column(name = "joined_at")
    private LocalDate joinedAt;

    @Column(name = "left_at")
    private LocalDate leftAt;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void update(
            String name,
            String email,
            String phone,
            String departmentCode,
            String departmentName,
            String position,
            EmploymentType employmentType,
            HrUserStatus hrStatus,
            LocalDate joinedAt,
            LocalDate leftAt
    ) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.departmentCode = departmentCode;
        this.departmentName = departmentName;
        this.position = position;
        this.employmentType = employmentType;
        this.hrStatus = hrStatus;
        this.joinedAt = joinedAt;
        this.leftAt = leftAt;
    }

    public void markAccountCreated(String username) {
        if (this.accountStatus == HrAccountStatus.CREATED) {
            throw new IllegalStateException("Account already created.");
        }
        this.accountStatus = HrAccountStatus.CREATED;
        this.accountUsername = username;
    }

    public void markAccountDisabled() {
        this.accountStatus = HrAccountStatus.DISABLED;
    }

    public boolean canCreateAccount() {
        return this.hrStatus == HrUserStatus.ACTIVE && this.accountStatus == HrAccountStatus.NOT_CREATED;
    }
}
