package com.example.authapp.security.rbac;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.AntPathMatcher;

import com.example.authapp.domain.authorization.entity.ApiPermissionRuleEntity;
import com.example.authapp.domain.authorization.repository.ApiPermissionRuleRepository;
import com.example.authapp.domain.authorization.repository.PermissionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RbacAuthorizationService {

    private static final String ANY_METHOD = "*";

    private final ApiPermissionRuleRepository apiPermissionRuleRepository;
    private final PermissionRepository permissionRepository;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Transactional(readOnly = true)
    public boolean isAllowed(Authentication authentication, String httpMethod, String requestPath) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || authentication.getName() == null
                || authentication.getName().isBlank()) {
            return false;
        }

        Set<String> requiredPermissions = findRequiredPermissions(httpMethod, requestPath);
        if (requiredPermissions.isEmpty()) {
            return false;
        }

        Set<String> userPermissions = findEffectivePermissions(authentication.getName());
        return requiredPermissions.stream().anyMatch(userPermissions::contains);
    }

    @Transactional(readOnly = true)
    public Set<String> findRequiredPermissions(String httpMethod, String requestPath) {
        String method = normalizeMethod(httpMethod);
        String path = normalizePath(requestPath);
        Set<String> permissions = new LinkedHashSet<>();

        collectMatchingPermissions(apiPermissionRuleRepository.findByHttpMethodAndEnabledTrueOrderBySortOrderAscPathPatternDesc(method), path, permissions);
        collectMatchingPermissions(apiPermissionRuleRepository.findByHttpMethodAndEnabledTrueOrderBySortOrderAscPathPatternDesc(ANY_METHOD), path, permissions);

        return permissions;
    }

    @Transactional(readOnly = true)
    public Set<String> findEffectivePermissions(String username) {
        Set<String> permissions = new LinkedHashSet<>();
        permissions.addAll(permissionRepository.findEnabledDirectPermissionCodesByUsername(username));
        permissions.addAll(permissionRepository.findEnabledGroupPermissionCodesByUsername(username));
        return permissions;
    }

    private void collectMatchingPermissions(List<ApiPermissionRuleEntity> rules, String requestPath, Set<String> permissions) {
        for (ApiPermissionRuleEntity rule : rules) {
            if (pathMatcher.match(rule.getPathPattern(), requestPath)) {
                permissions.add(rule.getPermissionCode());
            }
        }
    }

    private String normalizeMethod(String httpMethod) {
        return httpMethod == null || httpMethod.isBlank()
                ? ANY_METHOD
                : httpMethod.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizePath(String requestPath) {
        if (requestPath == null || requestPath.isBlank()) {
            return "/";
        }
        return requestPath.startsWith("/") ? requestPath : "/" + requestPath;
    }
}
