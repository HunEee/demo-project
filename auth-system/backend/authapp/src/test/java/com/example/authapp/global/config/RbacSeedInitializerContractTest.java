package com.example.authapp.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.util.AntPathMatcher;

import com.example.authapp.domain.authorization.entity.PermissionEntity;
import com.example.authapp.domain.authorization.entity.RoleEntity;
import com.example.authapp.domain.authorization.repository.ApiPermissionRuleRepository;
import com.example.authapp.domain.authorization.repository.PermissionRepository;
import com.example.authapp.domain.authorization.repository.RoleRepository;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.authapp.domain.authorization.entity.ApiPermissionRuleEntity;

class RbacSeedInitializerContractTest {

    private final RbacSeedInitializer initializer = new RbacSeedInitializer(null, null, null);
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Test
    void apiRulesOnlyReferenceSeededPermissions() {
        Set<String> permissionCodes = permissionSeeds().stream()
                .map(seed -> stringValue(seed, "code"))
                .collect(Collectors.toSet());

        assertThat(ruleSeeds())
                .extracting(seed -> stringValue(seed, "permissionCode"))
                .allSatisfy(permissionCode -> assertThat(permissionCodes).contains((String) permissionCode));
    }

    @Test
    void adminRoleReceivesEverySeededPermission() {
        PermissionRepository permissionRepository = mock(PermissionRepository.class);
        RoleRepository roleRepository = mock(RoleRepository.class);
        ApiPermissionRuleRepository apiPermissionRuleRepository = mock(ApiPermissionRuleRepository.class);
        RbacSeedInitializer seededInitializer = new RbacSeedInitializer(permissionRepository, roleRepository, apiPermissionRuleRepository);

        List<?> permissionSeeds = permissionSeeds(seededInitializer);
        List<String> permissionCodes = permissionSeeds.stream()
                .map(seed -> stringValue(seed, "code"))
                .toList();
        List<PermissionEntity> permissions = permissionSeeds.stream()
                .map(seed -> PermissionEntity.builder()
                        .code(stringValue(seed, "code"))
                        .name(stringValue(seed, "name"))
                        .category(stringValue(seed, "category"))
                        .enabled(true)
                        .build())
                .toList();
        RoleEntity adminRole = RoleEntity.builder()
                .name("ROLE_ADMIN")
                .enabled(true)
                .systemRole(true)
                .build();

        when(roleRepository.findWithPermissionsByName("ROLE_ADMIN")).thenReturn(Optional.of(adminRole));
        when(permissionRepository.findByCodeIn(permissionCodes)).thenReturn(permissions);

        invokeSeedAdminRolePermissions(seededInitializer, permissionSeeds);

        assertThat(adminRole.getPermissions())
                .extracting(PermissionEntity::getCode)
                .containsExactlyInAnyOrderElementsOf(permissionCodes);
    }

    @Test
    void restoresExistingSeededPermissionsToEnabledState() {
        PermissionRepository permissionRepository = mock(PermissionRepository.class);
        RoleRepository roleRepository = mock(RoleRepository.class);
        ApiPermissionRuleRepository apiPermissionRuleRepository = mock(ApiPermissionRuleRepository.class);
        RbacSeedInitializer seededInitializer = new RbacSeedInitializer(permissionRepository, roleRepository, apiPermissionRuleRepository);

        Object firstSeed = permissionSeeds(seededInitializer).get(0);
        PermissionEntity existingPermission = PermissionEntity.builder()
                .code(stringValue(firstSeed, "code"))
                .name("Disabled old permission")
                .category("OLD")
                .description("old")
                .sensitive(!booleanValue(firstSeed, "sensitive"))
                .enabled(false)
                .build();

        when(permissionRepository.findByCodeIn(permissionSeeds(seededInitializer).stream()
                .map(seed -> stringValue(seed, "code"))
                .toList())).thenReturn(List.of(existingPermission));

        invokeSeedPermissions(seededInitializer, permissionSeeds(seededInitializer));

        assertThat(existingPermission.isEnabled()).isTrue();
        assertThat(existingPermission.getName()).isEqualTo(stringValue(firstSeed, "name"));
        assertThat(existingPermission.getCategory()).isEqualTo(stringValue(firstSeed, "category"));
        assertThat(existingPermission.getDescription()).isEqualTo(stringValue(firstSeed, "description"));
        assertThat(existingPermission.isSensitive()).isEqualTo(booleanValue(firstSeed, "sensitive"));
    }

    @Test
    void restoresExistingSeededApiRulesToEnabledState() {
        PermissionRepository permissionRepository = mock(PermissionRepository.class);
        RoleRepository roleRepository = mock(RoleRepository.class);
        ApiPermissionRuleRepository apiPermissionRuleRepository = mock(ApiPermissionRuleRepository.class);
        RbacSeedInitializer seededInitializer = new RbacSeedInitializer(permissionRepository, roleRepository, apiPermissionRuleRepository);

        Object firstSeed = ruleSeeds().get(0);
        ApiPermissionRuleEntity existingRule = ApiPermissionRuleEntity.builder()
                .httpMethod(stringValue(firstSeed, "httpMethod"))
                .pathPattern(stringValue(firstSeed, "pathPattern"))
                .permissionCode(stringValue(firstSeed, "permissionCode"))
                .description("old")
                .enabled(false)
                .sortOrder(9999)
                .build();

        when(apiPermissionRuleRepository.findAll()).thenReturn(List.of(existingRule));

        invokeSeedRules(seededInitializer, ruleSeeds());

        assertThat(existingRule.isEnabled()).isTrue();
        assertThat(existingRule.getDescription()).isEqualTo(stringValue(firstSeed, "description"));
        assertThat(existingRule.getSortOrder()).isEqualTo(intValue(firstSeed, "sortOrder"));
    }

    @Test
    void seededRbacRulesCoverAdminFrontendApis() {
        List<ApiEndpoint> endpoints = List.of(
                new ApiEndpoint("GET", "/api/v1/admin/dashboard/summary"),
                new ApiEndpoint("GET", "/api/v1/admin/filter-options"),
                new ApiEndpoint("GET", "/api/v1/admin/users"),
                new ApiEndpoint("GET", "/api/v1/admin/users/alice"),
                new ApiEndpoint("POST", "/api/v1/admin/users"),
                new ApiEndpoint("PATCH", "/api/v1/admin/users/alice"),
                new ApiEndpoint("POST", "/api/v1/admin/users/alice/delete"),
                new ApiEndpoint("POST", "/api/v1/admin/users/alice/lock"),
                new ApiEndpoint("POST", "/api/v1/admin/users/alice/unlock"),
                new ApiEndpoint("POST", "/api/v1/admin/users/alice/disable"),
                new ApiEndpoint("POST", "/api/v1/admin/users/alice/enable"),
                new ApiEndpoint("POST", "/api/v1/admin/users/alice/tokens/revoke"),
                new ApiEndpoint("POST", "/api/v1/admin/users/alice/password/reset"),
                new ApiEndpoint("POST", "/api/v1/admin/users/alice/mfa/reset"),
                new ApiEndpoint("POST", "/api/v1/admin/users/alice/roles"),
                new ApiEndpoint("DELETE", "/api/v1/admin/users/alice/roles/1"),
                new ApiEndpoint("GET", "/api/v1/admin/departments"),
                new ApiEndpoint("POST", "/api/v1/admin/departments"),
                new ApiEndpoint("PATCH", "/api/v1/admin/departments/1"),
                new ApiEndpoint("POST", "/api/v1/admin/departments/1/disable"),
                new ApiEndpoint("GET", "/api/v1/admin/departments/1/users"),
                new ApiEndpoint("POST", "/api/v1/admin/departments/1/users"),
                new ApiEndpoint("PATCH", "/api/v1/admin/departments/1/users/alice"),
                new ApiEndpoint("DELETE", "/api/v1/admin/departments/1/users/alice"),
                new ApiEndpoint("GET", "/api/v1/admin/groups"),
                new ApiEndpoint("GET", "/api/v1/admin/groups/1"),
                new ApiEndpoint("POST", "/api/v1/admin/groups"),
                new ApiEndpoint("PATCH", "/api/v1/admin/groups/1"),
                new ApiEndpoint("POST", "/api/v1/admin/groups/1/disable"),
                new ApiEndpoint("POST", "/api/v1/admin/groups/1/members"),
                new ApiEndpoint("DELETE", "/api/v1/admin/groups/1/members/alice"),
                new ApiEndpoint("POST", "/api/v1/admin/groups/1/roles"),
                new ApiEndpoint("DELETE", "/api/v1/admin/groups/1/roles/1"),
                new ApiEndpoint("GET", "/api/v1/admin/roles"),
                new ApiEndpoint("GET", "/api/v1/admin/roles/1"),
                new ApiEndpoint("POST", "/api/v1/admin/roles"),
                new ApiEndpoint("PATCH", "/api/v1/admin/roles/1"),
                new ApiEndpoint("POST", "/api/v1/admin/roles/1/disable"),
                new ApiEndpoint("POST", "/api/v1/admin/roles/1/permissions"),
                new ApiEndpoint("DELETE", "/api/v1/admin/roles/1/permissions/1"),
                new ApiEndpoint("GET", "/api/v1/admin/permissions"),
                new ApiEndpoint("POST", "/api/v1/admin/permissions"),
                new ApiEndpoint("PATCH", "/api/v1/admin/permissions/1"),
                new ApiEndpoint("DELETE", "/api/v1/admin/permissions/1"),
                new ApiEndpoint("GET", "/api/v1/admin/role-assignment-history"),
                new ApiEndpoint("GET", "/api/v1/admin/api-permission-rules"),
                new ApiEndpoint("POST", "/api/v1/admin/api-permission-rules"),
                new ApiEndpoint("PATCH", "/api/v1/admin/api-permission-rules/1"),
                new ApiEndpoint("DELETE", "/api/v1/admin/api-permission-rules/1"),
                new ApiEndpoint("GET", "/api/v1/admin/audit-logs"),
                new ApiEndpoint("GET", "/api/v1/admin/login-history"),
                new ApiEndpoint("GET", "/api/v1/admin/security-events"),
                new ApiEndpoint("GET", "/api/v1/admin/incidents"),
                new ApiEndpoint("POST", "/api/v1/admin/incidents/1/resolve"),
                new ApiEndpoint("GET", "/api/v1/admin/sessions"),
                new ApiEndpoint("DELETE", "/api/v1/admin/sessions/1"),
                new ApiEndpoint("GET", "/api/v1/admin/risks"),
                new ApiEndpoint("GET", "/api/v1/admin/settings"),
                new ApiEndpoint("PATCH", "/api/v1/admin/settings")
        );

        assertThat(endpoints)
                .allSatisfy(endpoint -> assertThat(isCovered(endpoint))
                        .as("%s %s must be covered by seeded RBAC rules", endpoint.method(), endpoint.path())
                        .isTrue());
    }

    private boolean isCovered(ApiEndpoint endpoint) {
        return ruleSeeds().stream().anyMatch(seed ->
                (stringValue(seed, "httpMethod").equals(endpoint.method()) || stringValue(seed, "httpMethod").equals("*"))
                        && pathMatcher.match(stringValue(seed, "pathPattern"), endpoint.path())
        );
    }

    private List<?> permissionSeeds() {
        return permissionSeeds(initializer);
    }

    private List<?> permissionSeeds(RbacSeedInitializer target) {
        return invokeSeedList(target, "permissionSeeds");
    }

    private List<?> ruleSeeds() {
        return invokeSeedList(initializer, "ruleSeeds");
    }

    private List<?> invokeSeedList(RbacSeedInitializer target, String methodName) {
        try {
            Method method = RbacSeedInitializer.class.getDeclaredMethod(methodName);
            method.setAccessible(true);
            return (List<?>) method.invoke(target);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot inspect RBAC seed method: " + methodName, exception);
        }
    }

    private void invokeSeedAdminRolePermissions(RbacSeedInitializer target, List<?> permissionSeeds) {
        try {
            Method method = RbacSeedInitializer.class.getDeclaredMethod("seedAdminRolePermissions", List.class);
            method.setAccessible(true);
            method.invoke(target, permissionSeeds);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot inspect RBAC admin role permission seeding.", exception);
        }
    }

    private void invokeSeedPermissions(RbacSeedInitializer target, List<?> permissionSeeds) {
        try {
            Method method = RbacSeedInitializer.class.getDeclaredMethod("seedPermissions", List.class);
            method.setAccessible(true);
            method.invoke(target, permissionSeeds);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot inspect RBAC permission seeding.", exception);
        }
    }

    private void invokeSeedRules(RbacSeedInitializer target, List<?> ruleSeeds) {
        try {
            Method method = RbacSeedInitializer.class.getDeclaredMethod("seedRules", List.class);
            method.setAccessible(true);
            method.invoke(target, ruleSeeds);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot inspect RBAC rule seeding.", exception);
        }
    }

    private String stringValue(Object seed, String accessor) {
        try {
            Method method = seed.getClass().getDeclaredMethod(accessor);
            method.setAccessible(true);
            return (String) method.invoke(seed);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot inspect RBAC seed accessor: " + accessor, exception);
        }
    }

    private boolean booleanValue(Object seed, String accessor) {
        try {
            Method method = seed.getClass().getDeclaredMethod(accessor);
            method.setAccessible(true);
            return (boolean) method.invoke(seed);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot inspect RBAC seed accessor: " + accessor, exception);
        }
    }

    private int intValue(Object seed, String accessor) {
        try {
            Method method = seed.getClass().getDeclaredMethod(accessor);
            method.setAccessible(true);
            return (int) method.invoke(seed);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot inspect RBAC seed accessor: " + accessor, exception);
        }
    }

    private record ApiEndpoint(String method, String path) {
    }
}
