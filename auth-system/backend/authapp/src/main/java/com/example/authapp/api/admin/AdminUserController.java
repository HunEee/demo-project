package com.example.authapp.api.admin;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.authapp.application.admin.usecase.AdminSessionUseCase;
import com.example.authapp.application.admin.usecase.AdminUserUseCase;
import com.example.authapp.domain.admin.dto.AdminDuplicateCheckResponse;
import com.example.authapp.domain.admin.dto.AdminPasswordResetResponse;
import com.example.authapp.domain.admin.dto.AdminSessionResponse;
import com.example.authapp.domain.admin.dto.AdminUserCreateRequest;
import com.example.authapp.domain.admin.dto.AdminUserDetailResponse;
import com.example.authapp.domain.admin.dto.AdminUserResponse;
import com.example.authapp.domain.admin.dto.AdminUserStatusRequest;
import com.example.authapp.domain.admin.dto.AdminUserUpdateRequest;
import com.example.authapp.domain.authorization.dto.AdminRoleAssignmentRequest;
import com.example.authapp.application.admin.usecase.AdminRoleAssignmentUseCase;
import com.example.authapp.domain.audit.dto.PageResponseDTO;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserUseCase adminConsoleService;
    private final AdminSessionUseCase adminSessionUseCase;
    private final AdminRoleAssignmentUseCase roleAssignmentService;

    // 검색 조건과 상태 필터로 관리자 사용자 목록을 조회한다.
    @GetMapping
    public PageResponseDTO<AdminUserResponse> users(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "role", required = false) String role,
            @RequestParam(name = "departmentCode", required = false) String departmentCode,
            @RequestParam(name = "employmentType", required = false) String employmentType,
            @RequestParam(name = "directOnly", required = false) Boolean directOnly,
            @RequestParam(name = "authMethod", required = false) String authMethod,
            @RequestParam(name = "mfaEnabled", required = false) Boolean mfaEnabled,
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "email", required = false) String email,
            @RequestParam(name = "position", required = false) String position,
            @RequestParam(name = "locked", required = false) Boolean locked,
            @RequestParam(name = "lastLoginFrom", required = false) String lastLoginFrom,
            @RequestParam(name = "lastLoginTo", required = false) String lastLoginTo,
            @RequestParam(name = "sort", required = false) String sort,
            @RequestParam(name = "direction", defaultValue = "DESC") String direction
    ) {
        return new PageResponseDTO<>(adminConsoleService.users(
                page,
                size,
                keyword,
                status,
                role,
                sort,
                direction,
                departmentCode,
                employmentType,
                directOnly,
                authMethod,
                mfaEnabled,
                name,
                email,
                position,
                locked,
                lastLoginFrom,
                lastLoginTo
        ));
    }

    @PostMapping
    public AdminUserResponse create(@RequestBody AdminUserCreateRequest request) {
        return adminConsoleService.createUser(request);
    }

    @GetMapping({"/duplicate-check", "/exists"})
    public AdminDuplicateCheckResponse exists(@RequestParam(name = "username") String username) {
        return new AdminDuplicateCheckResponse("username", username, adminConsoleService.usernameExists(username));
    }

    // 사용자 상세 화면에 필요한 최근 활동 정보를 함께 반환한다.
    @GetMapping("/{username}")
    public AdminUserDetailResponse userDetail(@PathVariable(name = "username") String username) {
        return adminConsoleService.userDetail(username);
    }

    @PatchMapping("/{username}")
    public AdminUserResponse update(@PathVariable(name = "username") String username, @RequestBody AdminUserUpdateRequest request) {
        return adminConsoleService.updateUser(username, request);
    }

    @PatchMapping("/{username}/status")
    public AdminUserResponse updateStatus(
            @PathVariable(name = "username") String username,
            @RequestBody AdminUserStatusRequest request
    ) {
        return adminConsoleService.updateUserStatus(username, request);
    }

    @PatchMapping("/{username}/lock")
    public void lock(@PathVariable(name = "username") String username) {
        adminConsoleService.lockUser(username);
    }

    @PatchMapping("/{username}/unlock")
    public void unlock(@PathVariable(name = "username") String username) {
        adminConsoleService.unlockUser(username);
    }

    @PostMapping("/{username}/revoke-tokens")
    public void revokeTokens(@PathVariable(name = "username") String username) {
        adminSessionUseCase.revokeUserTokens(username);
    }

    @GetMapping("/{username}/sessions")
    public List<AdminSessionResponse> userSessions(@PathVariable(name = "username") String username) {
        return adminSessionUseCase.sessionsByUsername(username);
    }

    @DeleteMapping("/{username}/sessions")
    public void revokeUserSessions(@PathVariable(name = "username") String username) {
        adminSessionUseCase.revokeUserSessions(username);
    }

    @PostMapping("/{username}/reset-password")
    public AdminPasswordResetResponse resetPassword(@PathVariable(name = "username") String username) {
        return adminConsoleService.resetPassword(username);
    }

    @PostMapping("/{username}/reset-mfa")
    public void resetMfa(@PathVariable(name = "username") String username) {
        adminConsoleService.resetMfa(username);
    }

    @PostMapping("/{username}/roles")
    public void assignRole(@PathVariable(name = "username") String username, @RequestBody AdminRoleAssignmentRequest request) {
        roleAssignmentService.assignUserRole(username, request);
    }

    @DeleteMapping("/{username}/roles/{roleId}")
    public void removeRole(
            @PathVariable(name = "username") String username,
            @PathVariable(name = "roleId") Long roleId,
            @RequestParam(name = "reason", required = false) String reason
    ) {
        roleAssignmentService.revokeUserRole(username, roleId, reason);
    }
    
}
