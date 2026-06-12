package com.example.authapp.security.rbac;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import com.example.authapp.domain.authorization.entity.ApiPermissionRuleEntity;
import com.example.authapp.domain.authorization.repository.ApiPermissionRuleRepository;
import com.example.authapp.domain.authorization.repository.PermissionRepository;

@ExtendWith(MockitoExtension.class)
class RbacAuthorizationServiceTest {

    @Mock
    private ApiPermissionRuleRepository apiPermissionRuleRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @Test
    void deniesWhenNoApiRuleMatchesRequest() {
        RbacAuthorizationService service = new RbacAuthorizationService(apiPermissionRuleRepository, permissionRepository);
        when(apiPermissionRuleRepository.findByHttpMethodAndEnabledTrueOrderBySortOrderAscPathPatternDesc("GET")).thenReturn(List.of());
        when(apiPermissionRuleRepository.findByHttpMethodAndEnabledTrueOrderBySortOrderAscPathPatternDesc("*")).thenReturn(List.of());

        boolean allowed = service.isAllowed(authentication("operator1"), "GET", "/api/v1/admin/users");

        assertThat(allowed).isFalse();
        verifyNoInteractions(permissionRepository);
    }

    @Test
    void allowsWhenUserHasRequiredDirectPermission() {
        RbacAuthorizationService service = new RbacAuthorizationService(apiPermissionRuleRepository, permissionRepository);
        when(apiPermissionRuleRepository.findByHttpMethodAndEnabledTrueOrderBySortOrderAscPathPatternDesc("GET"))
                .thenReturn(List.of(rule("GET", "/api/v1/admin/users", "ADMIN_USERS_READ")));
        when(apiPermissionRuleRepository.findByHttpMethodAndEnabledTrueOrderBySortOrderAscPathPatternDesc("*")).thenReturn(List.of());
        when(permissionRepository.findEnabledDirectPermissionCodesByUsername("operator1")).thenReturn(List.of("ADMIN_USERS_READ"));
        when(permissionRepository.findEnabledGroupPermissionCodesByUsername("operator1")).thenReturn(List.of());

        boolean allowed = service.isAllowed(authentication("operator1"), "GET", "/api/v1/admin/users");

        assertThat(allowed).isTrue();
    }

    @Test
    void allowsCollectionRootRequestWhenRuleUsesWildcardChildrenPattern() {
        RbacAuthorizationService service = new RbacAuthorizationService(apiPermissionRuleRepository, permissionRepository);
        when(apiPermissionRuleRepository.findByHttpMethodAndEnabledTrueOrderBySortOrderAscPathPatternDesc("GET"))
                .thenReturn(List.of(rule("GET", "/api/v1/admin/roles/**", "ADMIN_ROLES_READ")));
        when(apiPermissionRuleRepository.findByHttpMethodAndEnabledTrueOrderBySortOrderAscPathPatternDesc("*")).thenReturn(List.of());
        when(permissionRepository.findEnabledDirectPermissionCodesByUsername("operator1")).thenReturn(List.of("ADMIN_ROLES_READ"));
        when(permissionRepository.findEnabledGroupPermissionCodesByUsername("operator1")).thenReturn(List.of());

        boolean allowed = service.isAllowed(authentication("operator1"), "GET", "/api/v1/admin/roles");

        assertThat(allowed).isTrue();
    }

    @Test
    void specificApiRuleTakesPrecedenceOverFallbackRule() {
        RbacAuthorizationService service = new RbacAuthorizationService(apiPermissionRuleRepository, permissionRepository);
        when(apiPermissionRuleRepository.findByHttpMethodAndEnabledTrueOrderBySortOrderAscPathPatternDesc("GET"))
                .thenReturn(List.of(
                        rule("GET", "/api/v1/admin/users", "ADMIN_USERS_READ", 10),
                        rule("GET", "/api/v1/admin/**", "ADMIN_ADMIN_READ", 1000)
                ));
        when(apiPermissionRuleRepository.findByHttpMethodAndEnabledTrueOrderBySortOrderAscPathPatternDesc("*")).thenReturn(List.of());
        when(permissionRepository.findEnabledDirectPermissionCodesByUsername("operator1")).thenReturn(List.of("ADMIN_ADMIN_READ"));
        when(permissionRepository.findEnabledGroupPermissionCodesByUsername("operator1")).thenReturn(List.of());

        boolean allowed = service.isAllowed(authentication("operator1"), "GET", "/api/v1/admin/users");

        assertThat(allowed).isFalse();
    }

    @Test
    void cachesApiRulesBetweenRequestPermissionLookups() {
        RbacAuthorizationService service = new RbacAuthorizationService(apiPermissionRuleRepository, permissionRepository);
        when(apiPermissionRuleRepository.findByHttpMethodAndEnabledTrueOrderBySortOrderAscPathPatternDesc("GET"))
                .thenReturn(List.of(rule("GET", "/api/v1/admin/users", "ADMIN_USERS_READ")));
        when(apiPermissionRuleRepository.findByHttpMethodAndEnabledTrueOrderBySortOrderAscPathPatternDesc("*")).thenReturn(List.of());

        assertThat(service.findRequiredPermissions("GET", "/api/v1/admin/users")).containsExactly("ADMIN_USERS_READ");
        assertThat(service.findRequiredPermissions("GET", "/api/v1/admin/users")).containsExactly("ADMIN_USERS_READ");

        verify(apiPermissionRuleRepository, times(1)).findByHttpMethodAndEnabledTrueOrderBySortOrderAscPathPatternDesc("GET");
        verify(apiPermissionRuleRepository, times(1)).findByHttpMethodAndEnabledTrueOrderBySortOrderAscPathPatternDesc("*");
    }

    @Test
    void reloadApiRuleCacheLoadsEnabledRulesInSingleQuery() {
        RbacAuthorizationService service = new RbacAuthorizationService(apiPermissionRuleRepository, permissionRepository);
        when(apiPermissionRuleRepository.findByEnabledTrueOrderByHttpMethodAscSortOrderAscPathPatternDesc())
                .thenReturn(List.of(
                        rule("GET", "/api/v1/admin/users", "ADMIN_USERS_READ"),
                        rule("POST", "/api/v1/admin/users", "ADMIN_USERS_WRITE")
                ));

        service.reloadApiRuleCache();

        assertThat(service.findRequiredPermissions("GET", "/api/v1/admin/users")).containsExactly("ADMIN_USERS_READ");
        assertThat(service.findRequiredPermissions("POST", "/api/v1/admin/users")).containsExactly("ADMIN_USERS_WRITE");
        verify(apiPermissionRuleRepository, times(1)).findByEnabledTrueOrderByHttpMethodAscSortOrderAscPathPatternDesc();
        verify(apiPermissionRuleRepository, never()).findByHttpMethodAndEnabledTrueOrderBySortOrderAscPathPatternDesc("GET");
        verify(apiPermissionRuleRepository, never()).findByHttpMethodAndEnabledTrueOrderBySortOrderAscPathPatternDesc("POST");
        verify(apiPermissionRuleRepository, never()).findByHttpMethodAndEnabledTrueOrderBySortOrderAscPathPatternDesc("*");
    }

    @Test
    void invalidatesCachedEffectivePermissionsForUser() {
        RbacAuthorizationService service = new RbacAuthorizationService(apiPermissionRuleRepository, permissionRepository);
        when(permissionRepository.findEnabledDirectPermissionCodesByUsername("operator1"))
                .thenReturn(List.of("ADMIN_USERS_READ"))
                .thenReturn(List.of("ADMIN_USERS_WRITE"));
        when(permissionRepository.findEnabledGroupPermissionCodesByUsername("operator1")).thenReturn(List.of());

        assertThat(service.findEffectivePermissions("operator1")).containsExactly("ADMIN_USERS_READ");
        assertThat(service.findEffectivePermissions("operator1")).containsExactly("ADMIN_USERS_READ");

        service.invalidateUserPermissionCache("operator1");

        assertThat(service.findEffectivePermissions("operator1")).containsExactly("ADMIN_USERS_WRITE");
        verify(permissionRepository, times(2)).findEnabledDirectPermissionCodesByUsername("operator1");
        verify(permissionRepository, times(2)).findEnabledGroupPermissionCodesByUsername("operator1");
    }

    @Test
    void allowsWhenUserHasRequiredPermissionThroughGroupRole() {
        RbacAuthorizationService service = new RbacAuthorizationService(apiPermissionRuleRepository, permissionRepository);
        when(apiPermissionRuleRepository.findByHttpMethodAndEnabledTrueOrderBySortOrderAscPathPatternDesc("DELETE"))
                .thenReturn(List.of(rule("DELETE", "/api/v1/admin/sessions/*", "ADMIN_AUDIT_WRITE")));
        when(apiPermissionRuleRepository.findByHttpMethodAndEnabledTrueOrderBySortOrderAscPathPatternDesc("*")).thenReturn(List.of());
        when(permissionRepository.findEnabledDirectPermissionCodesByUsername("operator1")).thenReturn(List.of());
        when(permissionRepository.findEnabledGroupPermissionCodesByUsername("operator1")).thenReturn(List.of("ADMIN_AUDIT_WRITE"));

        boolean allowed = service.isAllowed(authentication("operator1"), "DELETE", "/api/v1/admin/sessions/7");

        assertThat(allowed).isTrue();
    }

    @Test
    void deniesWhenEffectivePermissionsDoNotContainRequiredPermission() {
        RbacAuthorizationService service = new RbacAuthorizationService(apiPermissionRuleRepository, permissionRepository);
        when(apiPermissionRuleRepository.findByHttpMethodAndEnabledTrueOrderBySortOrderAscPathPatternDesc("PATCH"))
                .thenReturn(List.of(rule("PATCH", "/api/v1/admin/users/*", "ADMIN_USERS_WRITE")));
        when(apiPermissionRuleRepository.findByHttpMethodAndEnabledTrueOrderBySortOrderAscPathPatternDesc("*")).thenReturn(List.of());
        when(permissionRepository.findEnabledDirectPermissionCodesByUsername("operator1")).thenReturn(List.of("ADMIN_USERS_READ"));
        when(permissionRepository.findEnabledGroupPermissionCodesByUsername("operator1")).thenReturn(List.of());

        boolean allowed = service.isAllowed(authentication("operator1"), "PATCH", "/api/v1/admin/users/alice");

        assertThat(allowed).isFalse();
    }

    private UsernamePasswordAuthenticationToken authentication(String username) {
        return new UsernamePasswordAuthenticationToken(username, null, List.of());
    }

    private ApiPermissionRuleEntity rule(String method, String pathPattern, String permissionCode) {
        return rule(method, pathPattern, permissionCode, 10);
    }

    private ApiPermissionRuleEntity rule(String method, String pathPattern, String permissionCode, int sortOrder) {
        return ApiPermissionRuleEntity.builder()
                .httpMethod(method)
                .pathPattern(pathPattern)
                .permissionCode(permissionCode)
                .enabled(true)
                .sortOrder(sortOrder)
                .build();
    }
}
