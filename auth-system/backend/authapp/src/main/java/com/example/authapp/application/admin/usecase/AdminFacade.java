package com.example.authapp.application.admin.usecase;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.authapp.domain.jwt.service.RefreshTokenService;
import com.example.authapp.domain.user.entity.UserRoleType;
import com.example.authapp.domain.user.service.UserCommandService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminFacade {

    private final UserCommandService userCommandService;
    private final RefreshTokenService refreshTokenService;

    // 관리자 회원 탈퇴 처리
    public void deleteUser(String targetUsername, String role) {

        boolean isAdmin = role.equals("ROLE_"+UserRoleType.ROLE_ADMIN.name());

        if (!isAdmin) throw new AccessDeniedException("관리자만 삭제할 수 있습니다.");

        // 회원 삭제
        userCommandService.deleteUser(targetUsername);

        // refresh token 제거
        refreshTokenService.removeRefreshUser(targetUsername);
    }
    
    
}