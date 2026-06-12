package com.example.authapp.global.config;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
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
import com.example.authapp.security.rbac.RbacAuthorizationService;

import lombok.RequiredArgsConstructor;

@Component
@Order(10)
@RequiredArgsConstructor
public class RbacSeedInitializer implements CommandLineRunner {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final ApiPermissionRuleRepository apiPermissionRuleRepository;
    private RbacAuthorizationService rbacAuthorizationService;

    @Autowired
    public void setRbacAuthorizationService(RbacAuthorizationService rbacAuthorizationService) {
        this.rbacAuthorizationService = rbacAuthorizationService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        List<PermissionSeed> permissions = permissionSeeds();
        seedPermissions(permissions);
        seedAdminRolePermissions(permissions);
        seedRules(ruleSeeds());
        if (rbacAuthorizationService != null) {
            rbacAuthorizationService.reloadApiRuleCache();
            rbacAuthorizationService.invalidateAllUserPermissionCache();
        }
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
                new PermissionSeed("ADMIN_ADMIN_READ", "Admin read access", "ADMIN", "관리자 API 조회 기본 권한입니다.", false),
                new PermissionSeed("ADMIN_ADMIN_WRITE", "Admin write access", "ADMIN", "관리자 API 변경 기본 권한입니다.", true),
                new PermissionSeed("ADMIN_DASHBOARD_READ", "Admin dashboard read", "DASHBOARD", "관리자 대시보드 요약을 조회합니다.", false),
                new PermissionSeed("ADMIN_USERS_READ", "Admin users read", "USER", "관리 대상 사용자를 조회합니다.", false),
                new PermissionSeed("ADMIN_USERS_WRITE", "Admin users write", "USER", "관리 대상 사용자를 생성하거나 수정합니다.", true),
                new PermissionSeed("ADMIN_USERS_SECURITY", "Admin user security actions", "USER", "사용자 잠금, 잠금 해제, 토큰 회수, 비밀번호 초기화, MFA 초기화를 수행합니다.", true),
                new PermissionSeed("ADMIN_ROLES_READ", "Admin roles read", "AUTHORIZATION", "역할과 역할 부여 이력을 조회합니다.", false),
                new PermissionSeed("ADMIN_ROLES_WRITE", "Admin roles write", "AUTHORIZATION", "역할 생성, 수정, 비활성화, 부여, 회수를 수행합니다.", true),
                new PermissionSeed("ADMIN_PERMISSIONS_READ", "Admin permissions read", "AUTHORIZATION", "권한 목록과 API 권한 매핑을 조회합니다.", false),
                new PermissionSeed("ADMIN_PERMISSIONS_WRITE", "Admin permissions write", "AUTHORIZATION", "권한 목록과 API 권한 매핑을 생성, 수정, 삭제합니다.", true),
                new PermissionSeed("ADMIN_GROUPS_READ", "Admin groups read", "ORGANIZATION", "그룹을 조회합니다.", false),
                new PermissionSeed("ADMIN_GROUPS_WRITE", "Admin groups write", "ORGANIZATION", "그룹 생성, 수정, 비활성화, 역할 부여를 수행합니다.", true),
                new PermissionSeed("ADMIN_HR_USERS_READ", "Admin HR users read", "HR", "HR 사용자 기준정보를 조회합니다.", false),
                new PermissionSeed("ADMIN_HR_USERS_WRITE", "Admin HR users write", "HR", "HR 사용자 기준정보를 생성하거나 수정합니다.", true),
                new PermissionSeed("ADMIN_AUDIT_READ", "Admin audit read", "AUDIT", "감사 로그, 로그인 이력, 보안 이벤트, 사고, 세션, 위험 정보를 조회합니다.", false),
                new PermissionSeed("ADMIN_AUDIT_WRITE", "Admin audit write", "AUDIT", "보안 사고 처리 또는 세션 회수를 수행합니다.", true),
                new PermissionSeed("ADMIN_SETTINGS_READ", "Admin settings read", "SYSTEM", "관리자 설정을 조회합니다.", false),
                new PermissionSeed("ADMIN_SETTINGS_WRITE", "Admin settings write", "SYSTEM", "관리자 설정을 변경합니다.", true)
        );
    }

    private List<ApiRuleSeed> ruleSeeds() {
        return List.of(
                new ApiRuleSeed("GET", "/api/v1/admin/**", "ADMIN_ADMIN_READ", "관리자 API 조회 기본 규칙입니다.", 1000),
                new ApiRuleSeed("POST", "/api/v1/admin/**", "ADMIN_ADMIN_WRITE", "관리자 API 변경 기본 규칙입니다.", 1000),
                new ApiRuleSeed("PATCH", "/api/v1/admin/**", "ADMIN_ADMIN_WRITE", "관리자 API 변경 기본 규칙입니다.", 1000),
                new ApiRuleSeed("DELETE", "/api/v1/admin/**", "ADMIN_ADMIN_WRITE", "관리자 API 변경 기본 규칙입니다.", 1000),
                new ApiRuleSeed("GET", "/api/v1/admin/dashboard/**", "ADMIN_DASHBOARD_READ", "대시보드를 조회합니다.", 10),
                new ApiRuleSeed("GET", "/api/v1/admin/filter-options", "ADMIN_ADMIN_READ", "관리자 공통 필터 옵션을 조회합니다.", 10),
                new ApiRuleSeed("GET", "/api/v1/admin/filter-options", "ADMIN_DASHBOARD_READ", "관리자 공통 필터 옵션을 조회합니다.", 10),
                new ApiRuleSeed("GET", "/api/v1/admin/filter-options", "ADMIN_USERS_READ", "관리자 공통 필터 옵션을 조회합니다.", 10),
                new ApiRuleSeed("GET", "/api/v1/admin/filter-options", "ADMIN_ROLES_READ", "관리자 공통 필터 옵션을 조회합니다.", 10),
                new ApiRuleSeed("GET", "/api/v1/admin/filter-options", "ADMIN_PERMISSIONS_READ", "관리자 공통 필터 옵션을 조회합니다.", 10),
                new ApiRuleSeed("GET", "/api/v1/admin/filter-options", "ADMIN_GROUPS_READ", "관리자 공통 필터 옵션을 조회합니다.", 10),
                new ApiRuleSeed("GET", "/api/v1/admin/filter-options", "ADMIN_HR_USERS_READ", "관리자 공통 필터 옵션을 조회합니다.", 10),
                new ApiRuleSeed("GET", "/api/v1/admin/filter-options", "ADMIN_AUDIT_READ", "관리자 공통 필터 옵션을 조회합니다.", 10),
                new ApiRuleSeed("GET", "/api/v1/admin/filter-options", "ADMIN_SETTINGS_READ", "관리자 공통 필터 옵션을 조회합니다.", 10),
                new ApiRuleSeed("GET", "/api/v1/admin/users", "ADMIN_USERS_READ", "사용자 목록을 조회합니다.", 10),
                new ApiRuleSeed("GET", "/api/v1/users", "ADMIN_USERS_READ", "기존 사용자 조회 API를 허용합니다.", 10),
                new ApiRuleSeed("GET", "/api/v1/admin/users/*", "ADMIN_USERS_READ", "사용자 상세 정보를 조회합니다.", 10),
                new ApiRuleSeed("POST", "/api/v1/admin/users", "ADMIN_USERS_WRITE", "사용자를 생성합니다.", 10),
                new ApiRuleSeed("PATCH", "/api/v1/admin/users/*", "ADMIN_USERS_WRITE", "사용자를 수정합니다.", 10),
                new ApiRuleSeed("PATCH", "/api/v1/admin/users/*/status", "ADMIN_USERS_SECURITY", "사용자 상태를 변경합니다.", 10),
                new ApiRuleSeed("PATCH", "/api/v1/admin/users/*/lock", "ADMIN_USERS_SECURITY", "사용자를 잠급니다.", 10),
                new ApiRuleSeed("PATCH", "/api/v1/admin/users/*/unlock", "ADMIN_USERS_SECURITY", "사용자 잠금을 해제합니다.", 10),
                new ApiRuleSeed("POST", "/api/v1/admin/users/*/revoke-tokens", "ADMIN_USERS_SECURITY", "사용자 토큰을 회수합니다.", 10),
                new ApiRuleSeed("POST", "/api/v1/admin/users/*/reset-password", "ADMIN_USERS_SECURITY", "사용자 비밀번호를 초기화합니다.", 10),
                new ApiRuleSeed("POST", "/api/v1/admin/users/*/reset-mfa", "ADMIN_USERS_SECURITY", "사용자 MFA를 초기화합니다.", 10),
                new ApiRuleSeed("POST", "/api/v1/admin/users/*/roles", "ADMIN_ROLES_WRITE", "사용자에게 역할을 부여합니다.", 10),
                new ApiRuleSeed("DELETE", "/api/v1/admin/users/*/roles/*", "ADMIN_ROLES_WRITE", "사용자 역할을 회수합니다.", 10),
                new ApiRuleSeed("GET", "/api/v1/admin/roles/**", "ADMIN_ROLES_READ", "역할을 조회합니다.", 10),
                new ApiRuleSeed("POST", "/api/v1/admin/roles/**", "ADMIN_ROLES_WRITE", "역할을 변경합니다.", 10),
                new ApiRuleSeed("PATCH", "/api/v1/admin/roles/**", "ADMIN_ROLES_WRITE", "역할을 변경합니다.", 10),
                new ApiRuleSeed("DELETE", "/api/v1/admin/roles/**", "ADMIN_ROLES_WRITE", "역할을 변경합니다.", 10),
                new ApiRuleSeed("GET", "/api/v1/admin/role-assignment-history", "ADMIN_ROLES_READ", "역할 부여 이력을 조회합니다.", 10),
                new ApiRuleSeed("GET", "/api/v1/admin/permissions/**", "ADMIN_PERMISSIONS_READ", "권한을 조회합니다.", 10),
                new ApiRuleSeed("POST", "/api/v1/admin/permissions/**", "ADMIN_PERMISSIONS_WRITE", "권한을 변경합니다.", 10),
                new ApiRuleSeed("PATCH", "/api/v1/admin/permissions/**", "ADMIN_PERMISSIONS_WRITE", "권한을 변경합니다.", 10),
                new ApiRuleSeed("DELETE", "/api/v1/admin/permissions/**", "ADMIN_PERMISSIONS_WRITE", "권한을 변경합니다.", 10),
                new ApiRuleSeed("GET", "/api/v1/admin/api-permission-rules/**", "ADMIN_PERMISSIONS_READ", "API 권한 매핑을 조회합니다.", 10),
                new ApiRuleSeed("POST", "/api/v1/admin/api-permission-rules/**", "ADMIN_PERMISSIONS_WRITE", "API 권한 매핑을 변경합니다.", 10),
                new ApiRuleSeed("PATCH", "/api/v1/admin/api-permission-rules/**", "ADMIN_PERMISSIONS_WRITE", "API 권한 매핑을 변경합니다.", 10),
                new ApiRuleSeed("DELETE", "/api/v1/admin/api-permission-rules/**", "ADMIN_PERMISSIONS_WRITE", "API 권한 매핑을 변경합니다.", 10),
                new ApiRuleSeed("GET", "/api/v1/admin/groups/**", "ADMIN_GROUPS_READ", "그룹을 조회합니다.", 10),
                new ApiRuleSeed("POST", "/api/v1/admin/groups/**", "ADMIN_GROUPS_WRITE", "그룹을 변경합니다.", 10),
                new ApiRuleSeed("PATCH", "/api/v1/admin/groups/**", "ADMIN_GROUPS_WRITE", "그룹을 변경합니다.", 10),
                new ApiRuleSeed("DELETE", "/api/v1/admin/groups/**", "ADMIN_GROUPS_WRITE", "그룹을 변경합니다.", 10),
                new ApiRuleSeed("GET", "/api/v1/admin/hr-users/**", "ADMIN_HR_USERS_READ", "HR 사용자 기준정보를 조회합니다.", 10),
                new ApiRuleSeed("POST", "/api/v1/admin/hr-users/**", "ADMIN_HR_USERS_WRITE", "HR 사용자 기준정보를 변경합니다.", 10),
                new ApiRuleSeed("PATCH", "/api/v1/admin/hr-users/**", "ADMIN_HR_USERS_WRITE", "HR 사용자 기준정보를 변경합니다.", 10),
                new ApiRuleSeed("DELETE", "/api/v1/admin/hr-users/**", "ADMIN_HR_USERS_WRITE", "HR 사용자 기준정보를 삭제합니다.", 10),
                new ApiRuleSeed("GET", "/api/v1/admin/audit-logs", "ADMIN_AUDIT_READ", "감사 로그를 조회합니다.", 10),
                new ApiRuleSeed("GET", "/api/v1/admin/action-logs/**", "ADMIN_AUDIT_READ", "관리자 작업 로그를 조회합니다.", 10),
                new ApiRuleSeed("GET", "/api/v1/admin/login-history", "ADMIN_AUDIT_READ", "로그인 이력을 조회합니다.", 10),
                new ApiRuleSeed("GET", "/api/v1/admin/security-events", "ADMIN_AUDIT_READ", "보안 이벤트를 조회합니다.", 10),
                new ApiRuleSeed("GET", "/api/v1/admin/incidents", "ADMIN_AUDIT_READ", "보안 사고를 조회합니다.", 10),
                new ApiRuleSeed("POST", "/api/v1/admin/incidents/*/resolve", "ADMIN_AUDIT_WRITE", "보안 사고를 처리합니다.", 10),
                new ApiRuleSeed("GET", "/api/v1/admin/sessions", "ADMIN_AUDIT_READ", "세션을 조회합니다.", 10),
                new ApiRuleSeed("DELETE", "/api/v1/admin/sessions/*", "ADMIN_AUDIT_WRITE", "세션을 회수합니다.", 10),
                new ApiRuleSeed("GET", "/api/v1/admin/risks", "ADMIN_AUDIT_READ", "위험 정보를 조회합니다.", 10),
                new ApiRuleSeed("POST", "/api/v1/admin/risks/*/lock", "ADMIN_USERS_SECURITY", "위험 사용자를 잠급니다.", 10),
                new ApiRuleSeed("POST", "/api/v1/admin/risks/*/tokens/revoke", "ADMIN_USERS_SECURITY", "위험 사용자 토큰을 회수합니다.", 10),
                new ApiRuleSeed("POST", "/api/v1/admin/risks/*/mfa/require", "ADMIN_USERS_SECURITY", "위험 사용자에게 MFA를 요구합니다.", 10),
                new ApiRuleSeed("GET", "/api/v1/admin/mfa/policy", "ADMIN_SETTINGS_READ", "MFA 정책을 조회합니다.", 5),
                new ApiRuleSeed("PATCH", "/api/v1/admin/mfa/policy", "ADMIN_SETTINGS_WRITE", "MFA 정책을 변경합니다.", 5),
                new ApiRuleSeed("GET", "/api/v1/admin/mfa/**", "ADMIN_USERS_READ", "MFA 사용자 상태를 조회합니다.", 10),
                new ApiRuleSeed("POST", "/api/v1/admin/mfa/**", "ADMIN_USERS_SECURITY", "MFA 사용자 상태를 관리합니다.", 10),
                new ApiRuleSeed("DELETE", "/api/v1/admin/mfa/**", "ADMIN_USERS_SECURITY", "MFA 사용자 예외를 해제합니다.", 10),
                new ApiRuleSeed("GET", "/api/v1/admin/settings", "ADMIN_SETTINGS_READ", "설정을 조회합니다.", 10),
                new ApiRuleSeed("PATCH", "/api/v1/admin/settings", "ADMIN_SETTINGS_WRITE", "설정을 변경합니다.", 10),
                new ApiRuleSeed("GET", "/api/v1/admin/legacy/**", "ADMIN_ADMIN_READ", "기존 관리자 API를 조회합니다.", 20),
                new ApiRuleSeed("POST", "/api/v1/admin/legacy/**", "ADMIN_ADMIN_WRITE", "기존 관리자 API를 변경합니다.", 20)
        );
    }

    private record PermissionSeed(String code, String name, String category, String description, boolean sensitive) {
    }

    private record ApiRuleSeed(String httpMethod, String pathPattern, String permissionCode, String description, int sortOrder) {
    }
}
