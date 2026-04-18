package com.example.authapp.api;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.authapp.domain.admin.AdminService;
import com.example.authapp.domain.audit.entity.LoginHistory;
import com.example.authapp.domain.audit.entity.SecurityEvent;
import com.example.authapp.domain.user.entity.UserEntity;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/users")
    public List<UserEntity> getUsers() {
        return adminService.getAllUsers();
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
    public List<LoginHistory> getLoginHistory(@PathVariable String username) {
        return adminService.getLoginHistory(username);
    }

    @GetMapping("/security-event/{username}")
    public List<SecurityEvent> getSecurityEvents(@PathVariable String username) {
        return adminService.getSecurityEvents(username);
    }

    @PostMapping("/tokens/revoke/{username}")
    public void revokeTokens(@PathVariable String username) {
        adminService.revokeAllTokens(username);
    }
    
    
}
