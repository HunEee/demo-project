package com.example.authapp.domain.organization.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.authapp.domain.audit.entity.AdminActionLogEntity;
import com.example.authapp.domain.audit.entity.AdminActionType;
import com.example.authapp.domain.audit.repository.AdminActionLogRepository;
import com.example.authapp.domain.organization.dto.AdminDepartmentRequest;
import com.example.authapp.domain.organization.dto.AdminDepartmentResponse;
import com.example.authapp.domain.organization.dto.AdminDepartmentUserRequest;
import com.example.authapp.domain.organization.dto.AdminDepartmentUserResponse;
import com.example.authapp.domain.organization.entity.DepartmentEntity;
import com.example.authapp.domain.organization.repository.DepartmentRepository;
import com.example.authapp.domain.profile.entity.EmploymentType;
import com.example.authapp.domain.profile.entity.UserProfileEntity;
import com.example.authapp.domain.profile.entity.UserProfileStatus;
import com.example.authapp.domain.profile.repository.UserProfileRepository;
import com.example.authapp.domain.user.entity.UserEntity;
import com.example.authapp.domain.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminDepartmentService {

    private final DepartmentRepository departmentRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserRepository userRepository;
    private final AdminActionLogRepository adminActionLogRepository;

    @Transactional(readOnly = true)
    public List<AdminDepartmentResponse> list() {
        return departmentRepository.findAll().stream()
                .sorted(Comparator.comparing(DepartmentEntity::getDisplayOrder).thenComparing(DepartmentEntity::getName))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AdminDepartmentResponse create(AdminDepartmentRequest request) {
        requireText(request.name(), "Department name is required.");
        requireText(request.code(), "Department code is required.");
        if (departmentRepository.existsByCode(request.code())) {
            throw new IllegalArgumentException("Department code already exists.");
        }

        DepartmentEntity parent = findParent(request.parentId());
        validateManager(request.managerUsername());
        DepartmentEntity department = DepartmentEntity.builder()
                .name(request.name())
                .code(request.code())
                .parent(parent)
                .managerUsername(blankToNull(request.managerUsername()))
                .enabled(request.enabled() == null || request.enabled())
                .displayOrder(Optional.ofNullable(request.displayOrder()).orElse(0))
                .build();

        DepartmentEntity saved = departmentRepository.save(department);
        saveAudit("DEPARTMENT", targetId(saved), null, saved.getName(), AdminActionType.DEPARTMENT_CREATE, null, departmentState(saved), request.reason());
        return toResponse(saved);
    }

    @Transactional
    public AdminDepartmentResponse update(Long id, AdminDepartmentRequest request) {
        DepartmentEntity department = departmentRepository.findById(id).orElseThrow();
        String before = departmentState(department);
        DepartmentEntity parent = findParent(request.parentId());
        validateManager(request.managerUsername());

        department.update(
                defaultText(request.name(), department.getName()),
                defaultText(request.code(), department.getCode()),
                parent,
                blankToNull(request.managerUsername()),
                request.enabled() == null ? department.isEnabled() : request.enabled(),
                Optional.ofNullable(request.displayOrder()).orElse(department.getDisplayOrder())
        );
        saveAudit("DEPARTMENT", targetId(department), null, department.getName(), AdminActionType.DEPARTMENT_UPDATE, before, departmentState(department), request.reason());
        return toResponse(department);
    }

    @Transactional
    public void disable(Long id, String reason) {
        DepartmentEntity department = departmentRepository.findById(id).orElseThrow();
        String before = departmentState(department);
        department.disable();
        saveAudit("DEPARTMENT", targetId(department), null, department.getName(), AdminActionType.DEPARTMENT_DISABLE, before, departmentState(department), reason);
    }

    @Transactional(readOnly = true)
    public List<AdminDepartmentUserResponse> users(Long id) {
        return userProfileRepository.findByDepartmentId(id).stream()
                .map(profile -> AdminDepartmentUserResponse.from(profile, findUser(profile)))
                .toList();
    }

    @Transactional
    public AdminDepartmentUserResponse assignUser(Long id, AdminDepartmentUserRequest request) {
        requireText(request.username(), "Username is required.");
        DepartmentEntity department = findDepartment(id);
        UserEntity user = findExistingUser(request.username());
        UserProfileEntity profile = findOrCreateProfile(user.getUsername());
        String before = profileState(profile);

        applyDepartmentUserProfile(profile, department, request);
        UserProfileEntity saved = userProfileRepository.save(profile);
        saveAudit(
                "USER",
                user.getUsername(),
                user.getUsername(),
                defaultText(user.getNickname(), user.getUsername()),
                AdminActionType.DEPARTMENT_CHANGE,
                before,
                profileState(saved),
                defaultText(request.reason(), "Department user assignment")
        );
        return AdminDepartmentUserResponse.from(saved, user);
    }

    @Transactional
    public AdminDepartmentUserResponse updateUser(Long id, String username, AdminDepartmentUserRequest request) {
        DepartmentEntity department = findDepartment(id);
        UserEntity user = findExistingUser(username);
        UserProfileEntity profile = findOrCreateProfile(username);
        String before = profileState(profile);

        applyDepartmentUserProfile(profile, department, request);
        UserProfileEntity saved = userProfileRepository.save(profile);
        saveAudit(
                "USER",
                user.getUsername(),
                user.getUsername(),
                defaultText(user.getNickname(), user.getUsername()),
                AdminActionType.DEPARTMENT_CHANGE,
                before,
                profileState(saved),
                defaultText(request.reason(), "Department user profile update")
        );
        return AdminDepartmentUserResponse.from(saved, user);
    }

    @Transactional
    public void removeUser(Long id, String username, String reason) {
        findDepartment(id);
        UserEntity user = findExistingUser(username);
        UserProfileEntity profile = userProfileRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User is not assigned to department."));
        if (profile.getDepartment() == null || profile.getDepartment().getId() == null || !profile.getDepartment().getId().equals(id)) {
            throw new IllegalArgumentException("User is not assigned to department.");
        }
        String before = profileState(profile);
        profile.updateAdminProfile(
                profile.getEmployeeNo(),
                null,
                profile.getPosition(),
                Optional.ofNullable(profile.getEmploymentType()).orElse(EmploymentType.UNKNOWN),
                Optional.ofNullable(profile.getStatus()).orElse(UserProfileStatus.ACTIVE),
                profile.getExpiresAt()
        );
        userProfileRepository.save(profile);
        saveAudit(
                "USER",
                user.getUsername(),
                user.getUsername(),
                defaultText(user.getNickname(), user.getUsername()),
                AdminActionType.DEPARTMENT_CHANGE,
                before,
                profileState(profile),
                defaultText(reason, "Department user removal")
        );
    }

    private AdminDepartmentResponse toResponse(DepartmentEntity department) {
        Long departmentId = department.getId();
        long userCount = departmentId == null ? 0 : userProfileRepository.countByDepartmentId(departmentId);
        return AdminDepartmentResponse.from(department, userCount);
    }

    private DepartmentEntity findParent(Long parentId) {
        if (parentId == null) return null;
        return findDepartment(parentId);
    }

    private DepartmentEntity findDepartment(Long id) {
        return departmentRepository.findById(id).orElseThrow();
    }

    private UserEntity findUser(UserProfileEntity profile) {
        return userRepository.findByUsername(profile.getUsername()).orElse(null);
    }

    private UserEntity findExistingUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
    }

    private UserProfileEntity findOrCreateProfile(String username) {
        return userProfileRepository.findByUsername(username)
                .orElseGet(() -> UserProfileEntity.builder()
                        .username(username)
                        .employmentType(EmploymentType.UNKNOWN)
                        .status(UserProfileStatus.ACTIVE)
                        .build());
    }

    private void applyDepartmentUserProfile(
            UserProfileEntity profile,
            DepartmentEntity department,
            AdminDepartmentUserRequest request
    ) {
        profile.updateAdminProfile(
                request.employeeNo() == null ? profile.getEmployeeNo() : blankToNull(request.employeeNo()),
                department,
                request.position() == null ? profile.getPosition() : blankToNull(request.position()),
                parseEmploymentType(request.employmentType(), profile.getEmploymentType()),
                parseProfileStatus(request.status(), profile.getStatus()),
                parseDateTime(request.expiresAt(), profile.getExpiresAt())
        );
    }

    private void validateManager(String managerUsername) {
        if (managerUsername == null || managerUsername.isBlank()) return;
        userRepository.findByUsername(managerUsername).orElseThrow();
    }

    private void saveAudit(
            String targetType,
            String targetId,
            String targetUsername,
            String targetName,
            AdminActionType actionType,
            String beforeValue,
            String afterValue,
            String reason
    ) {
        adminActionLogRepository.save(AdminActionLogEntity.builder()
                .actorUsername(currentActor())
                .targetType(targetType)
                .targetId(targetId)
                .targetUsername(targetUsername)
                .targetName(targetName)
                .actionType(actionType)
                .reason(reason)
                .beforeValue(beforeValue)
                .afterValue(afterValue)
                .ipAddress("UNKNOWN")
                .device("UNKNOWN")
                .build());
    }

    private String currentActor() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) return "UNKNOWN";
        return authentication.getName();
    }

    private String departmentState(DepartmentEntity department) {
        return "{\"id\":\"" + targetId(department)
                + "\",\"code\":\"" + safe(department.getCode())
                + "\",\"enabled\":" + department.isEnabled()
                + ",\"managerUsername\":\"" + safe(department.getManagerUsername())
                + "\"}";
    }

    private String profileState(UserProfileEntity profile) {
        DepartmentEntity department = profile.getDepartment();
        return "{\"username\":\"" + safe(profile.getUsername())
                + "\",\"departmentId\":\"" + (department != null && department.getId() != null ? department.getId() : "")
                + "\",\"departmentName\":\"" + (department != null ? safe(department.getName()) : "")
                + "\",\"employeeNo\":\"" + safe(profile.getEmployeeNo())
                + "\",\"position\":\"" + safe(profile.getPosition())
                + "\",\"employmentType\":\"" + Optional.ofNullable(profile.getEmploymentType()).map(Enum::name).orElse("")
                + "\",\"status\":\"" + Optional.ofNullable(profile.getStatus()).map(Enum::name).orElse("")
                + "\"}";
    }

    private String targetId(DepartmentEntity department) {
        return department.getId() != null ? String.valueOf(department.getId()) : department.getCode();
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private EmploymentType parseEmploymentType(String value, EmploymentType fallback) {
        if (value == null || value.isBlank()) {
            return Optional.ofNullable(fallback).orElse(EmploymentType.UNKNOWN);
        }
        return EmploymentType.valueOf(value.toUpperCase(Locale.ROOT));
    }

    private UserProfileStatus parseProfileStatus(String value, UserProfileStatus fallback) {
        if (value == null || value.isBlank()) {
            return Optional.ofNullable(fallback).orElse(UserProfileStatus.ACTIVE);
        }
        return UserProfileStatus.valueOf(value.toUpperCase(Locale.ROOT));
    }

    private LocalDateTime parseDateTime(String value, LocalDateTime fallback) {
        if (value == null) return fallback;
        if (value.isBlank()) return null;
        if (value.length() == 10) return LocalDate.parse(value).atStartOfDay();
        return LocalDateTime.parse(value);
    }

    private String safe(String value) {
        return value == null ? "" : value.replace("\"", "\\\"");
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(message);
    }
}
