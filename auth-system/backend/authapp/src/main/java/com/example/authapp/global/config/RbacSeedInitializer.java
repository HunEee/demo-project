package com.example.authapp.global.config;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.authapp.domain.authorization.entity.ApiPermissionRuleEntity;
import com.example.authapp.domain.authorization.entity.PermissionEntity;
import com.example.authapp.domain.authorization.entity.RoleEntity;
import com.example.authapp.domain.authorization.repository.ApiPermissionRuleRepository;
import com.example.authapp.domain.authorization.repository.PermissionRepository;
import com.example.authapp.domain.authorization.repository.RoleRepository;

import lombok.RequiredArgsConstructor;

@Component
@Order(10)
@RequiredArgsConstructor
public class RbacSeedInitializer implements CommandLineRunner {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final ApiPermissionRuleRepository apiPermissionRuleRepository;

    @Override
    @Transactional
    public void run(String... args) {
        List<PermissionSeed> permissions = permissionSeeds();
        seedPermissions(permissions);
        seedAdminRolePermissions(permissions);
        seedRules(ruleSeeds());
    }

    private void seedPermissions(List<PermissionSeed> seeds) {
        Map<String, PermissionEntity> existingPermissions = permissionRepository.findByCodeIn(seeds.stream().map(PermissionSeed::code).toList())
                .stream()
                .collect(Collectors.toMap(PermissionEntity::getCode, permission -> permission));

        for (PermissionSeed seed : seeds) {
            PermissionEntity existingPermission = existingPermissions.get(seed.code());
            if (existingPermission != null && (
                    !existingPermission.isEnabled()
                            || !seed.name().equals(existingPermission.getName())
                            || !seed.category().equals(existingPermission.getCategory())
                            || !safeEquals(seed.description(), existingPermission.getDescription())
                            || seed.sensitive() != existingPermission.isSensitive()
            )) {
                existingPermission.update(
                        seed.code(),
                        seed.name(),
                        seed.category(),
                        seed.description(),
                        seed.sensitive(),
                        true
                );
            }
        }

        List<PermissionEntity> missingPermissions = seeds.stream()
                .filter(seed -> !existingPermissions.containsKey(seed.code()))
                .map(seed -> PermissionEntity.builder()
                        .code(seed.code())
                        .name(seed.name())
                        .category(seed.category())
                        .description(seed.description())
                        .sensitive(seed.sensitive())
                        .enabled(true)
                        .build())
                .toList();

        if (!missingPermissions.isEmpty()) {
            permissionRepository.saveAll(missingPermissions);
        }
    }

    private void seedAdminRolePermissions(List<PermissionSeed> permissions) {
        RoleEntity adminRole = roleRepository.findWithPermissionsByName("ROLE_ADMIN").orElse(null);
        if (adminRole == null) {
            return;
        }

        Set<String> assignedCodes = adminRole.getPermissions().stream()
                .map(PermissionEntity::getCode)
                .collect(Collectors.toSet());
        List<String> missingCodes = permissions.stream()
                .map(PermissionSeed::code)
                .filter(code -> !assignedCodes.contains(code))
                .toList();
        if (missingCodes.isEmpty()) {
            return;
        }

        permissionRepository.findByCodeIn(missingCodes).forEach(adminRole::addPermission);
    }

    private void seedRules(List<ApiRuleSeed> seeds) {
        Map<String, ApiPermissionRuleEntity> existingRules = apiPermissionRuleRepository.findAll().stream()
                .collect(Collectors.toMap(
                        rule -> ruleKey(rule.getHttpMethod(), rule.getPathPattern(), rule.getPermissionCode()),
                        rule -> rule,
                        (left, right) -> left
                ));

        for (ApiRuleSeed seed : seeds) {
            ApiPermissionRuleEntity existingRule = existingRules.get(ruleKey(seed.httpMethod(), seed.pathPattern(), seed.permissionCode()));
            if (existingRule != null && (
                    !existingRule.isEnabled()
                            || !safeEquals(seed.description(), existingRule.getDescription())
                            || seed.sortOrder() != existingRule.getSortOrder()
            )) {
                existingRule.update(
                        seed.httpMethod(),
                        seed.pathPattern(),
                        seed.permissionCode(),
                        seed.description(),
                        true,
                        seed.sortOrder()
                );
            }
        }

        List<ApiPermissionRuleEntity> missingRules = seeds.stream()
                .filter(seed -> !existingRules.containsKey(ruleKey(seed.httpMethod(), seed.pathPattern(), seed.permissionCode())))
                .map(seed -> ApiPermissionRuleEntity.builder()
                        .httpMethod(seed.httpMethod())
                        .pathPattern(seed.pathPattern())
                        .permissionCode(seed.permissionCode())
                        .description(seed.description())
                        .enabled(true)
                        .sortOrder(seed.sortOrder())
                        .build())
                .toList();

        if (!missingRules.isEmpty()) {
            apiPermissionRuleRepository.saveAll(missingRules);
        }
    }

    private String ruleKey(String httpMethod, String pathPattern, String permissionCode) {
        return httpMethod + "\n" + pathPattern + "\n" + permissionCode;
    }

    private boolean safeEquals(String left, String right) {
        if (left == null) {
            return right == null;
        }
        return left.equals(right);
    }

    private List<PermissionSeed> permissionSeeds() {
        return List.of(
                new PermissionSeed("ADMIN_ADMIN_READ", "Admin read access", "ADMIN", "Read-only fallback permission for admin APIs.", false),
                new PermissionSeed("ADMIN_ADMIN_WRITE", "Admin write access", "ADMIN", "Write fallback permission for admin APIs.", true),
                new PermissionSeed("ADMIN_DASHBOARD_READ", "Admin dashboard read", "DASHBOARD", "Read admin dashboard summary.", false),
                new PermissionSeed("ADMIN_USERS_READ", "Admin users read", "USER", "Read managed users.", false),
                new PermissionSeed("ADMIN_USERS_WRITE", "Admin users write", "USER", "Create or update managed users.", true),
                new PermissionSeed("ADMIN_USERS_SECURITY", "Admin user security actions", "USER", "Lock, unlock, revoke token, reset password, or reset MFA.", true),
                new PermissionSeed("ADMIN_ROLES_READ", "Admin roles read", "AUTHORIZATION", "Read roles and role assignment history.", false),
                new PermissionSeed("ADMIN_ROLES_WRITE", "Admin roles write", "AUTHORIZATION", "Create, update, disable, assign, or revoke roles.", true),
                new PermissionSeed("ADMIN_PERMISSIONS_READ", "Admin permissions read", "AUTHORIZATION", "Read permission catalog.", false),
                new PermissionSeed("ADMIN_PERMISSIONS_WRITE", "Admin permissions write", "AUTHORIZATION", "Create, update, or delete permission catalog entries.", true),
                new PermissionSeed("ADMIN_GROUPS_READ", "Admin groups read", "ORGANIZATION", "Read groups.", false),
                new PermissionSeed("ADMIN_GROUPS_WRITE", "Admin groups write", "ORGANIZATION", "Create, update, disable, or assign groups.", true),
                new PermissionSeed("ADMIN_HR_USERS_READ", "Admin HR users read", "HR", "Read HR user master records.", false),
                new PermissionSeed("ADMIN_HR_USERS_WRITE", "Admin HR users write", "HR", "Create or update HR user master records.", true),
                new PermissionSeed("ADMIN_AUDIT_READ", "Admin audit read", "AUDIT", "Read audit logs, login history, security events, incidents, sessions, and risk.", false),
                new PermissionSeed("ADMIN_AUDIT_WRITE", "Admin audit write", "AUDIT", "Resolve incidents or revoke sessions.", true),
                new PermissionSeed("ADMIN_SETTINGS_READ", "Admin settings read", "SYSTEM", "Read admin settings.", false),
                new PermissionSeed("ADMIN_SETTINGS_WRITE", "Admin settings write", "SYSTEM", "Update admin settings.", true)
        );
    }

    private List<ApiRuleSeed> ruleSeeds() {
        return List.of(
                new ApiRuleSeed("GET", "/api/v1/admin/**", "ADMIN_ADMIN_READ", "Fallback read permission for admin APIs.", 1000),
                new ApiRuleSeed("POST", "/api/v1/admin/**", "ADMIN_ADMIN_WRITE", "Fallback write permission for admin APIs.", 1000),
                new ApiRuleSeed("PATCH", "/api/v1/admin/**", "ADMIN_ADMIN_WRITE", "Fallback write permission for admin APIs.", 1000),
                new ApiRuleSeed("DELETE", "/api/v1/admin/**", "ADMIN_ADMIN_WRITE", "Fallback write permission for admin APIs.", 1000),
                new ApiRuleSeed("GET", "/api/v1/admin/dashboard/**", "ADMIN_DASHBOARD_READ", "Read dashboard.", 10),
                new ApiRuleSeed("GET", "/api/v1/admin/filter-options", "ADMIN_ADMIN_READ", "Read admin filter options.", 10),
                new ApiRuleSeed("GET", "/api/v1/admin/users", "ADMIN_USERS_READ", "Read users.", 10),
                new ApiRuleSeed("GET", "/api/v1/users", "ADMIN_USERS_READ", "Read legacy users endpoint.", 10),
                new ApiRuleSeed("GET", "/api/v1/admin/users/*", "ADMIN_USERS_READ", "Read user detail.", 10),
                new ApiRuleSeed("POST", "/api/v1/admin/users", "ADMIN_USERS_WRITE", "Create users.", 10),
                new ApiRuleSeed("PATCH", "/api/v1/admin/users/*", "ADMIN_USERS_WRITE", "Update users.", 10),
                new ApiRuleSeed("POST", "/api/v1/admin/users/*/delete", "ADMIN_USERS_WRITE", "Delete users.", 10),
                new ApiRuleSeed("POST", "/api/v1/admin/users/*/lock", "ADMIN_USERS_SECURITY", "Lock users.", 10),
                new ApiRuleSeed("POST", "/api/v1/admin/users/*/unlock", "ADMIN_USERS_SECURITY", "Unlock users.", 10),
                new ApiRuleSeed("POST", "/api/v1/admin/users/*/disable", "ADMIN_USERS_SECURITY", "Disable users.", 10),
                new ApiRuleSeed("POST", "/api/v1/admin/users/*/enable", "ADMIN_USERS_SECURITY", "Enable users.", 10),
                new ApiRuleSeed("POST", "/api/v1/admin/users/*/tokens/revoke", "ADMIN_USERS_SECURITY", "Revoke user tokens.", 10),
                new ApiRuleSeed("POST", "/api/v1/admin/users/*/password/reset", "ADMIN_USERS_SECURITY", "Reset user password.", 10),
                new ApiRuleSeed("POST", "/api/v1/admin/users/*/mfa/reset", "ADMIN_USERS_SECURITY", "Reset user MFA.", 10),
                new ApiRuleSeed("POST", "/api/v1/admin/users/*/roles", "ADMIN_ROLES_WRITE", "Assign user role.", 10),
                new ApiRuleSeed("DELETE", "/api/v1/admin/users/*/roles/*", "ADMIN_ROLES_WRITE", "Revoke user role.", 10),
                new ApiRuleSeed("GET", "/api/v1/admin/roles/**", "ADMIN_ROLES_READ", "Read roles.", 10),
                new ApiRuleSeed("POST", "/api/v1/admin/roles/**", "ADMIN_ROLES_WRITE", "Write roles.", 10),
                new ApiRuleSeed("PATCH", "/api/v1/admin/roles/**", "ADMIN_ROLES_WRITE", "Write roles.", 10),
                new ApiRuleSeed("DELETE", "/api/v1/admin/roles/**", "ADMIN_ROLES_WRITE", "Write roles.", 10),
                new ApiRuleSeed("GET", "/api/v1/admin/role-assignment-history", "ADMIN_ROLES_READ", "Read role assignment history.", 10),
                new ApiRuleSeed("GET", "/api/v1/admin/permissions/**", "ADMIN_PERMISSIONS_READ", "Read permissions.", 10),
                new ApiRuleSeed("POST", "/api/v1/admin/permissions/**", "ADMIN_PERMISSIONS_WRITE", "Write permissions.", 10),
                new ApiRuleSeed("PATCH", "/api/v1/admin/permissions/**", "ADMIN_PERMISSIONS_WRITE", "Write permissions.", 10),
                new ApiRuleSeed("DELETE", "/api/v1/admin/permissions/**", "ADMIN_PERMISSIONS_WRITE", "Write permissions.", 10),
                new ApiRuleSeed("GET", "/api/v1/admin/api-permission-rules/**", "ADMIN_PERMISSIONS_READ", "Read API permission rules.", 10),
                new ApiRuleSeed("POST", "/api/v1/admin/api-permission-rules/**", "ADMIN_PERMISSIONS_WRITE", "Write API permission rules.", 10),
                new ApiRuleSeed("PATCH", "/api/v1/admin/api-permission-rules/**", "ADMIN_PERMISSIONS_WRITE", "Write API permission rules.", 10),
                new ApiRuleSeed("DELETE", "/api/v1/admin/api-permission-rules/**", "ADMIN_PERMISSIONS_WRITE", "Write API permission rules.", 10),
                new ApiRuleSeed("GET", "/api/v1/admin/groups/**", "ADMIN_GROUPS_READ", "Read groups.", 10),
                new ApiRuleSeed("POST", "/api/v1/admin/groups/**", "ADMIN_GROUPS_WRITE", "Write groups.", 10),
                new ApiRuleSeed("PATCH", "/api/v1/admin/groups/**", "ADMIN_GROUPS_WRITE", "Write groups.", 10),
                new ApiRuleSeed("DELETE", "/api/v1/admin/groups/**", "ADMIN_GROUPS_WRITE", "Write groups.", 10),
                new ApiRuleSeed("GET", "/api/v1/admin/hr-users/**", "ADMIN_HR_USERS_READ", "Read HR users.", 10),
                new ApiRuleSeed("POST", "/api/v1/admin/hr-users/**", "ADMIN_HR_USERS_WRITE", "Write HR users.", 10),
                new ApiRuleSeed("PATCH", "/api/v1/admin/hr-users/**", "ADMIN_HR_USERS_WRITE", "Write HR users.", 10),
                new ApiRuleSeed("DELETE", "/api/v1/admin/hr-users/**", "ADMIN_HR_USERS_WRITE", "Delete HR users.", 10),
                new ApiRuleSeed("GET", "/api/v1/admin/audit-logs", "ADMIN_AUDIT_READ", "Read audit logs.", 10),
                new ApiRuleSeed("GET", "/api/v1/admin/login-history", "ADMIN_AUDIT_READ", "Read login history.", 10),
                new ApiRuleSeed("GET", "/api/v1/admin/security-events", "ADMIN_AUDIT_READ", "Read security events.", 10),
                new ApiRuleSeed("GET", "/api/v1/admin/incidents", "ADMIN_AUDIT_READ", "Read incidents.", 10),
                new ApiRuleSeed("POST", "/api/v1/admin/incidents/*/resolve", "ADMIN_AUDIT_WRITE", "Resolve incidents.", 10),
                new ApiRuleSeed("GET", "/api/v1/admin/sessions", "ADMIN_AUDIT_READ", "Read sessions.", 10),
                new ApiRuleSeed("DELETE", "/api/v1/admin/sessions/*", "ADMIN_AUDIT_WRITE", "Revoke sessions.", 10),
                new ApiRuleSeed("GET", "/api/v1/admin/risks", "ADMIN_AUDIT_READ", "Read risks.", 10),
                new ApiRuleSeed("GET", "/api/v1/admin/settings", "ADMIN_SETTINGS_READ", "Read settings.", 10),
                new ApiRuleSeed("PATCH", "/api/v1/admin/settings", "ADMIN_SETTINGS_WRITE", "Update settings.", 10),
                new ApiRuleSeed("GET", "/api/v1/admin/legacy/**", "ADMIN_ADMIN_READ", "Read legacy admin APIs.", 20),
                new ApiRuleSeed("POST", "/api/v1/admin/legacy/**", "ADMIN_ADMIN_WRITE", "Write legacy admin APIs.", 20)
        );
    }

    private record PermissionSeed(String code, String name, String category, String description, boolean sensitive) {
    }

    private record ApiRuleSeed(String httpMethod, String pathPattern, String permissionCode, String description, int sortOrder) {
    }
}
