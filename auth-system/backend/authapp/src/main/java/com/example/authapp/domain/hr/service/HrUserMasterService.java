package com.example.authapp.domain.hr.service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.authapp.domain.hr.dto.HrUserMasterRequest;
import com.example.authapp.domain.hr.dto.HrUserMasterResponse;
import com.example.authapp.domain.hr.entity.EmploymentType;
import com.example.authapp.domain.hr.entity.HrAccountStatus;
import com.example.authapp.domain.hr.entity.HrUserMasterEntity;
import com.example.authapp.domain.hr.entity.HrUserStatus;
import com.example.authapp.domain.hr.repository.HrUserMasterRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HrUserMasterService {

    private final HrUserMasterRepository hrUserMasterRepository;

    @Transactional(readOnly = true)
    public List<HrUserMasterResponse> list(String keyword, String accountStatus) {
        return hrUserMasterRepository.findAll().stream()
                .filter(item -> keywordMatches(item, keyword))
                .filter(item -> accountStatus == null || accountStatus.isBlank()
                        || item.getAccountStatus().name().equalsIgnoreCase(accountStatus))
                .sorted(Comparator.comparing(HrUserMasterEntity::getEmployeeNo, String.CASE_INSENSITIVE_ORDER))
                .map(HrUserMasterResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HrUserMasterResponse> accountCandidates() {
        return hrUserMasterRepository.findByAccountStatus(HrAccountStatus.NOT_CREATED).stream()
                .filter(HrUserMasterEntity::canCreateAccount)
                .sorted(Comparator.comparing(HrUserMasterEntity::getEmployeeNo, String.CASE_INSENSITIVE_ORDER))
                .map(HrUserMasterResponse::from)
                .toList();
    }

    @Transactional
    public HrUserMasterResponse create(HrUserMasterRequest request) {
        requireText(request.employeeNo(), "Employee number is required.");
        requireText(request.name(), "Name is required.");
        requireText(request.email(), "Email is required.");
        if (hrUserMasterRepository.existsByEmployeeNo(request.employeeNo())) {
            throw new IllegalArgumentException("Employee number already exists.");
        }
        if (hrUserMasterRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already exists.");
        }
        if (request.phone() != null && !request.phone().isBlank() && hrUserMasterRepository.existsByPhone(request.phone())) {
            throw new IllegalArgumentException("Phone already exists.");
        }

        HrUserMasterEntity saved = hrUserMasterRepository.save(HrUserMasterEntity.builder()
                .employeeNo(request.employeeNo().trim())
                .name(request.name().trim())
                .email(request.email().trim())
                .phone(blankToNull(request.phone()))
                .departmentCode(blankToNull(request.departmentCode()))
                .departmentName(blankToNull(request.departmentName()))
                .position(blankToNull(request.position()))
                .employmentType(parseEmploymentType(request.employmentType()))
                .hrStatus(parseHrStatus(request.hrStatus()))
                .joinedAt(parseDate(request.joinedAt()))
                .leftAt(parseDate(request.leftAt()))
                .build());
        return HrUserMasterResponse.from(saved);
    }

    @Transactional
    public HrUserMasterResponse update(Long id, HrUserMasterRequest request) {
        HrUserMasterEntity entity = hrUserMasterRepository.findById(id).orElseThrow();
        requireText(request.name(), "Name is required.");
        requireText(request.email(), "Email is required.");
        if (!entity.getEmail().equals(request.email()) && hrUserMasterRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already exists.");
        }
        if (request.phone() != null
                && !request.phone().isBlank()
                && !request.phone().equals(entity.getPhone())
                && hrUserMasterRepository.existsByPhone(request.phone())) {
            throw new IllegalArgumentException("Phone already exists.");
        }
        entity.update(
                request.name().trim(),
                request.email().trim(),
                blankToNull(request.phone()),
                blankToNull(request.departmentCode()),
                blankToNull(request.departmentName()),
                blankToNull(request.position()),
                parseEmploymentType(request.employmentType()),
                parseHrStatus(request.hrStatus()),
                parseDate(request.joinedAt()),
                parseDate(request.leftAt())
        );
        return HrUserMasterResponse.from(entity);
    }

    @Transactional(readOnly = true)
    public boolean exists(String field, String value) {
        if (field == null || value == null || value.isBlank()) return false;
        return switch (field.toLowerCase(Locale.ROOT)) {
            case "employeeno", "employee_no" -> hrUserMasterRepository.existsByEmployeeNo(value.trim());
            case "email" -> hrUserMasterRepository.existsByEmail(value.trim());
            case "phone" -> hrUserMasterRepository.existsByPhone(value.trim());
            case "accountusername", "account_username", "username" -> hrUserMasterRepository.findByAccountUsername(value.trim()).isPresent();
            default -> throw new IllegalArgumentException("Unsupported duplicate check field.");
        };
    }

    @Transactional
    public void delete(Long id) {
        HrUserMasterEntity entity = hrUserMasterRepository.findById(id).orElseThrow();
        if (entity.getAccountStatus() != HrAccountStatus.NOT_CREATED) {
            throw new IllegalStateException("Only HR users without an account can be deleted.");
        }
        hrUserMasterRepository.delete(entity);
    }

    @Transactional
    public HrUserMasterResponse markAccountCreated(String employeeNo, String username) {
        HrUserMasterEntity entity = hrUserMasterRepository.findByEmployeeNo(employeeNo).orElseThrow();
        entity.markAccountCreated(username);
        return HrUserMasterResponse.from(entity);
    }

    private boolean keywordMatches(HrUserMasterEntity item, String keyword) {
        if (keyword == null || keyword.isBlank()) return true;
        String normalized = keyword.toLowerCase(Locale.ROOT);
        return contains(item.getEmployeeNo(), normalized)
                || contains(item.getName(), normalized)
                || contains(item.getEmail(), normalized)
                || contains(item.getDepartmentCode(), normalized)
                || contains(item.getDepartmentName(), normalized)
                || contains(item.getPosition(), normalized)
                || contains(item.getAccountUsername(), normalized);
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private EmploymentType parseEmploymentType(String value) {
        if (value == null || value.isBlank()) return EmploymentType.UNKNOWN;
        return EmploymentType.valueOf(value.toUpperCase(Locale.ROOT));
    }

    private HrUserStatus parseHrStatus(String value) {
        if (value == null || value.isBlank()) return HrUserStatus.ACTIVE;
        return HrUserStatus.valueOf(value.toUpperCase(Locale.ROOT));
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        return LocalDate.parse(value);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(message);
    }
}
