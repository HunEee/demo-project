package com.example.authapp.application.admin.usecase;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.authapp.domain.authorization.dto.AdminApiPermissionRuleRequest;
import com.example.authapp.domain.authorization.dto.AdminApiPermissionRuleResponse;
import com.example.authapp.domain.authorization.entity.ApiPermissionRuleEntity;
import com.example.authapp.domain.authorization.repository.ApiPermissionRuleRepository;
import com.example.authapp.domain.authorization.repository.PermissionRepository;
import com.example.authapp.security.rbac.RbacAuthorizationService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminApiPermissionRuleService {

    private static final String ANY_METHOD = "*";

    private final ApiPermissionRuleRepository apiPermissionRuleRepository;
    private final PermissionRepository permissionRepository;
    private final RbacAuthorizationService rbacAuthorizationService;

    @Transactional(readOnly = true)
    public List<AdminApiPermissionRuleResponse> list() {
        return apiPermissionRuleRepository.findAll().stream()
                .sorted(Comparator.comparingInt(ApiPermissionRuleEntity::getSortOrder)
                        .thenComparing(ApiPermissionRuleEntity::getHttpMethod)
                        .thenComparing(ApiPermissionRuleEntity::getPathPattern))
                .map(AdminApiPermissionRuleResponse::from)
                .toList();
    }

    @Transactional
    public AdminApiPermissionRuleResponse create(AdminApiPermissionRuleRequest request) {
        String httpMethod = normalizeMethod(request.httpMethod());
        String pathPattern = normalizePathPattern(request.pathPattern());
        String permissionCode = normalizePermissionCode(request.permissionCode());
        requirePermission(permissionCode);

        if (apiPermissionRuleRepository.existsByHttpMethodAndPathPatternAndPermissionCode(httpMethod, pathPattern, permissionCode)) {
            throw new IllegalArgumentException("API permission rule already exists.");
        }

        ApiPermissionRuleEntity rule = ApiPermissionRuleEntity.builder()
                .httpMethod(httpMethod)
                .pathPattern(pathPattern)
                .permissionCode(permissionCode)
                .description(blankToNull(request.description()))
                .enabled(request.enabled() == null || request.enabled())
                .sortOrder(request.sortOrder() == null ? 100 : request.sortOrder())
                .build();

        ApiPermissionRuleEntity saved = apiPermissionRuleRepository.save(rule);
        rbacAuthorizationService.reloadApiRuleCache();
        return AdminApiPermissionRuleResponse.from(saved);
    }

    @Transactional
    public AdminApiPermissionRuleResponse update(Long id, AdminApiPermissionRuleRequest request) {
        ApiPermissionRuleEntity rule = apiPermissionRuleRepository.findById(id).orElseThrow();
        String httpMethod = request.httpMethod() == null ? rule.getHttpMethod() : normalizeMethod(request.httpMethod());
        String pathPattern = request.pathPattern() == null ? rule.getPathPattern() : normalizePathPattern(request.pathPattern());
        String permissionCode = request.permissionCode() == null ? rule.getPermissionCode() : normalizePermissionCode(request.permissionCode());
        requirePermission(permissionCode);

        rule.update(
                httpMethod,
                pathPattern,
                permissionCode,
                request.description() == null ? rule.getDescription() : blankToNull(request.description()),
                request.enabled() == null ? rule.isEnabled() : request.enabled(),
                request.sortOrder() == null ? rule.getSortOrder() : request.sortOrder()
        );

        rbacAuthorizationService.reloadApiRuleCache();
        return AdminApiPermissionRuleResponse.from(rule);
    }

    @Transactional
    public void delete(Long id) {
        apiPermissionRuleRepository.deleteById(id);
        rbacAuthorizationService.reloadApiRuleCache();
    }

    private void requirePermission(String permissionCode) {
        if (!permissionRepository.existsByCode(permissionCode)) {
            throw new IllegalArgumentException("Permission does not exist.");
        }
    }

    private String normalizeMethod(String httpMethod) {
        requireText(httpMethod, "HTTP method is required.");
        String method = httpMethod.trim().toUpperCase(Locale.ROOT);
        if (ANY_METHOD.equals(method)) {
            return method;
        }
        return switch (method) {
            case "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD" -> method;
            default -> throw new IllegalArgumentException("Unsupported HTTP method.");
        };
    }

    private String normalizePathPattern(String pathPattern) {
        requireText(pathPattern, "Path pattern is required.");
        String normalized = pathPattern.trim();
        return normalized.startsWith("/") ? normalized : "/" + normalized;
    }

    private String normalizePermissionCode(String permissionCode) {
        requireText(permissionCode, "Permission code is required.");
        return permissionCode.trim().toUpperCase(Locale.ROOT);
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(message);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
