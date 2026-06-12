package com.example.authapp.security.rbac;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
    private final Map<String, List<ApiPermissionRuleEntity>> apiRulesByMethod = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> effectivePermissionsByUsername = new ConcurrentHashMap<>();
    private volatile boolean apiRuleCacheWarmed;

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
        List<ApiPermissionRuleEntity> matchingRules = new ArrayList<>();

        collectMatchingRules(findCachedRules(method), path, matchingRules);
        collectMatchingRules(findCachedRules(ANY_METHOD), path, matchingRules);

        if (matchingRules.isEmpty()) {
            return Set.of();
        }

        int bestSortOrder = matchingRules.stream()
                .mapToInt(ApiPermissionRuleEntity::getSortOrder)
                .min()
                .orElse(Integer.MAX_VALUE);

        Set<String> permissions = new LinkedHashSet<>();
        matchingRules.stream()
                .filter(rule -> rule.getSortOrder() == bestSortOrder)
                .map(ApiPermissionRuleEntity::getPermissionCode)
                .forEach(permissions::add);
        return permissions;
    }

    @Transactional(readOnly = true)
    public Set<String> findEffectivePermissions(String username) {
        String normalizedUsername = username == null ? "" : username.trim();
        if (normalizedUsername.isBlank()) {
            return Set.of();
        }
        return effectivePermissionsByUsername.computeIfAbsent(normalizedUsername, this::loadEffectivePermissions);
    }

    public void warmupApiRuleCache() {
        Map<String, List<ApiPermissionRuleEntity>> rulesByMethod = new ConcurrentHashMap<>();
        for (ApiPermissionRuleEntity rule : apiPermissionRuleRepository.findByEnabledTrueOrderByHttpMethodAscSortOrderAscPathPatternDesc()) {
            String method = normalizeMethod(rule.getHttpMethod());
            rulesByMethod.computeIfAbsent(method, ignored -> new ArrayList<>()).add(rule);
        }

        apiRulesByMethod.clear();
        rulesByMethod.forEach((method, rules) -> apiRulesByMethod.put(method, List.copyOf(rules)));
        apiRuleCacheWarmed = true;
    }

    public void invalidateApiRuleCache() {
        apiRulesByMethod.clear();
        apiRuleCacheWarmed = false;
    }

    public void reloadApiRuleCache() {
        invalidateApiRuleCache();
        warmupApiRuleCache();
    }

    public void invalidateUserPermissionCache(String username) {
        if (username == null || username.isBlank()) {
            return;
        }
        effectivePermissionsByUsername.remove(username.trim());
    }

    public void invalidateAllUserPermissionCache() {
        effectivePermissionsByUsername.clear();
    }

    public void invalidateAllCaches() {
        invalidateApiRuleCache();
        invalidateAllUserPermissionCache();
    }

    private List<ApiPermissionRuleEntity> findCachedRules(String method) {
        if (apiRuleCacheWarmed) {
            return apiRulesByMethod.getOrDefault(method, List.of());
        }
        return apiRulesByMethod.computeIfAbsent(method, apiPermissionRuleRepository::findByHttpMethodAndEnabledTrueOrderBySortOrderAscPathPatternDesc);
    }

    private Set<String> loadEffectivePermissions(String username) {
        Set<String> permissions = new LinkedHashSet<>();
        permissions.addAll(permissionRepository.findEnabledDirectPermissionCodesByUsername(username));
        permissions.addAll(permissionRepository.findEnabledGroupPermissionCodesByUsername(username));
        return permissions;
    }

    private void collectMatchingRules(List<ApiPermissionRuleEntity> rules, String requestPath, List<ApiPermissionRuleEntity> matchingRules) {
        for (ApiPermissionRuleEntity rule : rules) {
            if (pathMatcher.match(rule.getPathPattern(), requestPath)) {
                matchingRules.add(rule);
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
