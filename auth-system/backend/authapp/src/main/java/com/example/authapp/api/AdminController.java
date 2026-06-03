package com.example.authapp.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.authapp.domain.admin.AdminService;
import com.example.authapp.domain.admin.dto.AdminUserResponse;
import com.example.authapp.domain.audit.entity.LoginHistoryEntity;
import com.example.authapp.domain.audit.entity.AuthEventLogEntity;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/legacy")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/users")
    public List<AdminUserResponse> getUsers() {
        return adminService.getAllUsers()
                .stream()
                .map(AdminUserResponse::from)
                .toList();
    }

    @PostMapping("/users/{id}/lock")
    public void lockUser(@PathVariable Long id) {
        adminService.lockUser(id);
    }

    @PostMapping("/users/{id}/unlock")
    public void unlockUser(@PathVariable Long id) {
        adminService.unlockUser(id);
    }

    @GetMapping("/login-history/{username}")
    public List<LoginHistoryEntity> getLoginHistory(@PathVariable String username) {
        return adminService.getLoginHistory(username);
    }

    @GetMapping("/security-event/{username}")
    public List<AuthEventLogEntity> getSecurityEvents(@PathVariable String username) {
        return adminService.getSecurityEvents(username);
    }

    @PostMapping("/tokens/revoke/{username}")
    public void revokeTokens(@PathVariable String username) {
        adminService.revokeAllTokens(username);
    }
    
    
}
