package com.example.authapp.application.admin.usecase;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.authapp.application.user.usecase.UserCommandService;
import com.example.authapp.domain.jwt.service.RefreshTokenService;
import com.example.authapp.domain.user.entity.UserRoleType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminFacade {

    private final UserCommandService userCommandService;
    private final RefreshTokenService refreshTokenService;

    // 관리자가 회원을 탈퇴 처리한다.
    public void deleteUser(String targetUsername, String role) {
        boolean isAdmin = role.equals("ROLE_" + UserRoleType.ROLE_ADMIN.name());
        if (!isAdmin) {
            throw new AccessDeniedException("관리자만 삭제할 수 있습니다.");
        }

        userCommandService.deleteUser(targetUsername);
        refreshTokenService.removeRefreshUser(targetUsername);
    }
}
