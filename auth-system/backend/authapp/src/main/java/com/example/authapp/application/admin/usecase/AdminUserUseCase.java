package com.example.authapp.application.admin.usecase;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import com.example.authapp.domain.admin.dto.AdminPasswordResetResponse;
import com.example.authapp.domain.admin.dto.AdminUserCreateRequest;
import com.example.authapp.domain.admin.dto.AdminUserDetailResponse;
import com.example.authapp.domain.admin.dto.AdminUserResponse;
import com.example.authapp.domain.admin.dto.AdminUserStatusRequest;
import com.example.authapp.domain.admin.dto.AdminUserUpdateRequest;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminUserUseCase {

    private final AdminConsoleUseCase adminConsoleUseCase;

    public Page<AdminUserResponse> users(
            int page,
            int size,
            String keyword,
            String status,
            String role,
            String sort,
            String direction,
            String departmentCode,
            String employmentType,
            Boolean directOnly,
            String authMethod,
            Boolean mfaEnabled,
            String name,
            String email,
            String position,
            Boolean locked,
            String lastLoginFrom,
            String lastLoginTo
    ) {
        return adminConsoleUseCase.users(page, size, keyword, status, role, sort, direction, departmentCode, employmentType, directOnly, authMethod, mfaEnabled, name, email, position, locked, lastLoginFrom, lastLoginTo);
    }

    public AdminUserResponse createUser(AdminUserCreateRequest request) {
        return adminConsoleUseCase.createUser(request);
    }

    public boolean usernameExists(String username) {
        return adminConsoleUseCase.usernameExists(username);
    }

    public AdminUserDetailResponse userDetail(String username) {
        return adminConsoleUseCase.userDetail(username);
    }

    public AdminUserResponse updateUser(String username, AdminUserUpdateRequest request) {
        return adminConsoleUseCase.updateUser(username, request);
    }

    public AdminUserResponse updateUserStatus(String username, AdminUserStatusRequest request) {
        return adminConsoleUseCase.updateUserStatus(username, request);
    }

    public void lockUser(String username) {
        adminConsoleUseCase.lockUser(username);
    }

    public void unlockUser(String username) {
        adminConsoleUseCase.unlockUser(username);
    }

    public AdminPasswordResetResponse resetPassword(String username) {
        return adminConsoleUseCase.resetPassword(username);
    }

    public void resetMfa(String username) {
        adminConsoleUseCase.resetMfa(username);
    }
}
