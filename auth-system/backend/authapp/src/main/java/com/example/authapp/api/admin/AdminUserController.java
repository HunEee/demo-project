package com.example.authapp.api.admin;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.authapp.domain.admin.AdminConsoleService;
import com.example.authapp.domain.admin.dto.AdminPasswordResetResponse;
import com.example.authapp.domain.admin.dto.AdminUserCreateRequest;
import com.example.authapp.domain.admin.dto.AdminUserDetailResponse;
import com.example.authapp.domain.admin.dto.AdminUserResponse;
import com.example.authapp.domain.admin.dto.AdminUserUpdateRequest;
import com.example.authapp.domain.authorization.dto.AdminRoleAssignmentRequest;
import com.example.authapp.domain.authorization.service.RoleAssignmentService;
import com.example.authapp.domain.audit.dto.PageResponseDTO;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminConsoleService adminConsoleService;
    private final RoleAssignmentService roleAssignmentService;

    // 검색/상태/권한 필터가 가능한 관리자 사용자 목록
    @GetMapping
    public PageResponseDTO<AdminUserResponse> users(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "role", required = false) String role,
            @RequestParam(name = "departmentId", required = false) Long departmentId,
            @RequestParam(name = "employmentType", required = false) String employmentType,
            @RequestParam(name = "expiresBefore", required = false) String expiresBefore,
            @RequestParam(name = "mfaEnabled", required = false) Boolean mfaEnabled,
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
                departmentId,
                employmentType,
                expiresBefore,
                mfaEnabled
        ));
    }

    @PostMapping
    public AdminUserResponse create(@RequestBody AdminUserCreateRequest request) {
        return adminConsoleService.createUser(request);
    }

    // 사용자 상세 화면에 필요한 최근 활동 정보를 함께 반환
    @GetMapping("/{username}")
    public AdminUserDetailResponse userDetail(@PathVariable String username) {
        return adminConsoleService.userDetail(username);
    }

    @PatchMapping("/{username}")
    public AdminUserResponse update(@PathVariable String username, @RequestBody AdminUserUpdateRequest request) {
        return adminConsoleService.updateUser(username, request);
    }

    @PostMapping("/{username}/delete")
    public void delete(
            @PathVariable String username,
            @RequestParam(name = "reason", required = false) String reason
    ) {
        adminConsoleService.deleteUser(username, reason);
    }

    @PostMapping("/{username}/lock")
    public void lock(@PathVariable String username) {
        adminConsoleService.lockUser(username);
    }

    @PostMapping("/{username}/unlock")
    public void unlock(@PathVariable String username) {
        adminConsoleService.unlockUser(username);
    }

    @PostMapping("/{username}/disable")
    public void disable(@PathVariable String username) {
        adminConsoleService.disableUser(username);
    }

    @PostMapping("/{username}/enable")
    public void enable(@PathVariable String username) {
        adminConsoleService.enableUser(username);
    }

    @PostMapping("/{username}/tokens/revoke")
    public void revokeTokens(@PathVariable String username) {
        adminConsoleService.revokeUserTokens(username);
    }

    @PostMapping("/{username}/password/reset")
    public AdminPasswordResetResponse resetPassword(@PathVariable String username) {
        return adminConsoleService.resetPassword(username);
    }

    @PostMapping("/{username}/mfa/reset")
    public void resetMfa(@PathVariable String username) {
        adminConsoleService.resetMfa(username);
    }

    @PostMapping("/{username}/roles")
    public void assignRole(@PathVariable String username, @RequestBody AdminRoleAssignmentRequest request) {
        roleAssignmentService.assignUserRole(username, request);
    }

    @DeleteMapping("/{username}/roles/{roleId}")
    public void removeRole(
            @PathVariable String username,
            @PathVariable Long roleId,
            @RequestParam(name = "reason", required = false) String reason
    ) {
        roleAssignmentService.revokeUserRole(username, roleId, reason);
    }
    
}
