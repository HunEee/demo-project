package com.example.authapp.security.rbac;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
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
        return ApiPermissionRuleEntity.builder()
                .httpMethod(method)
                .pathPattern(pathPattern)
                .permissionCode(permissionCode)
                .enabled(true)
                .sortOrder(10)
                .build();
    }
}
