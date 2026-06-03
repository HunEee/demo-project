package com.example.authapp.domain.authorization.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.authapp.domain.authorization.dto.AdminPermissionRequest;
import com.example.authapp.domain.authorization.dto.AdminPermissionResponse;
import com.example.authapp.domain.authorization.entity.PermissionEntity;
import com.example.authapp.domain.authorization.repository.PermissionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminPermissionService {

    private final PermissionRepository permissionRepository;

    @Transactional(readOnly = true)
    public List<AdminPermissionResponse> list() {
        return permissionRepository.findAll().stream()
                .sorted(Comparator.comparing(PermissionEntity::getCategory, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(PermissionEntity::getCode, String.CASE_INSENSITIVE_ORDER))
                .map(AdminPermissionResponse::from)
                .toList();
    }

    @Transactional
    public AdminPermissionResponse create(AdminPermissionRequest request) {
        requireText(request.code(), "Permission code is required.");
        requireText(request.name(), "Permission name is required.");
        requireText(request.category(), "Permission category is required.");
        if (permissionRepository.existsByCode(request.code())) {
            throw new IllegalArgumentException("Permission already exists.");
        }
        PermissionEntity permission = PermissionEntity.builder()
                .code(request.code())
                .name(request.name())
                .category(request.category())
                .description(blankToNull(request.description()))
                .sensitive(request.sensitive() != null && request.sensitive())
                .enabled(request.enabled() == null || request.enabled())
                .build();
        return AdminPermissionResponse.from(permissionRepository.save(permission));
    }

    @Transactional
    public AdminPermissionResponse update(Long id, AdminPermissionRequest request) {
        PermissionEntity permission = permissionRepository.findById(id).orElseThrow();
        permission.update(
                defaultText(request.code(), permission.getCode()),
                defaultText(request.name(), permission.getName()),
                defaultText(request.category(), permission.getCategory()),
                request.description() == null ? permission.getDescription() : blankToNull(request.description()),
                request.sensitive() == null ? permission.isSensitive() : request.sensitive(),
                request.enabled() == null ? permission.isEnabled() : request.enabled()
        );
        return AdminPermissionResponse.from(permission);
    }

    @Transactional
    public void delete(Long id) {
        permissionRepository.deleteById(id);
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(message);
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
