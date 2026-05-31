package com.example.authapp.api.admin;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.authapp.domain.admin.AdminConsoleService;
import com.example.authapp.domain.admin.dto.AdminPasswordResetResponse;
import com.example.authapp.domain.admin.dto.AdminUserDetailResponse;
import com.example.authapp.domain.admin.dto.AdminUserResponse;
import com.example.authapp.domain.audit.dto.PageResponseDTO;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminConsoleService adminConsoleService;

    // 검색/상태/권한 필터가 가능한 관리자 사용자 목록
    @GetMapping
    public PageResponseDTO<AdminUserResponse> users(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "role", required = false) String role,
            @RequestParam(name = "sort", required = false) String sort,
            @RequestParam(name = "direction", defaultValue = "DESC") String direction
    ) {
        return new PageResponseDTO<>(adminConsoleService.users(page, size, keyword, status, role, sort, direction));
    }

    // 사용자 상세 화면에 필요한 최근 활동 정보를 함께 반환
    @GetMapping("/{username}")
    public AdminUserDetailResponse userDetail(@PathVariable String username) {
        return adminConsoleService.userDetail(username);
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
    
}
