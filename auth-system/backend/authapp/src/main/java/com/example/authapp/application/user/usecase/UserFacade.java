package com.example.authapp.application.user.usecase;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.authapp.application.auth.usecase.EmailCodeService;
import com.example.authapp.application.user.dto.FindUsernameRequest;
import com.example.authapp.application.user.dto.SignupRequest;
import com.example.authapp.application.user.dto.UpdateUserProfileRequest;
import com.example.authapp.domain.audit.service.AuthEventLogService;
import com.example.authapp.domain.jwt.service.RefreshTokenService;
import com.example.authapp.domain.user.dto.UserResponse;
import com.example.authapp.domain.user.dto.user.CheckUsernameRequest;
import com.example.authapp.domain.user.service.PasswordService;
import com.example.authapp.domain.user.service.UserQueryService;
import com.example.authapp.domain.verificatoin.dto.VerifyCodeRequest;
import com.example.authapp.domain.verificatoin.entity.EmailCodePurpose;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UserFacade {

    private final EmailCodeService emailCodeService;
    private final RefreshTokenService refreshTokenService;
    private final AuthEventLogService authEventLogService;
    private final UserCommandService userCommandService;
    private final UserQueryService userQueryService;
    private final PasswordService passwordService;

    // 인증 코드를 검증하고 회원가입을 처리한다.
    public Long signup(SignupRequest request) {
        emailCodeService.verifyCode(
                new VerifyCodeRequest(request.email(), request.verificationCode(), EmailCodePurpose.SIGNUP),
                true
        );
        return userCommandService.createUser(request);
    }

    // 내 프로필 정보를 수정한다.
    public Long updateMyProfile(String username, UpdateUserProfileRequest request) {
        return userCommandService.updateUser(username, request);
    }

    // 내 계정을 탈퇴 처리하고 refresh token을 폐기한다.
    public void deleteMyAccount(String username) {
        userCommandService.deleteMyAccount(username);
        refreshTokenService.removeRefreshUser(username);
    }

    // 사용자명 중복 여부를 조회한다.
    @Transactional(readOnly = true)
    public boolean existsUsername(CheckUsernameRequest request) {
        return userQueryService.existUsername(request.username());
    }

    // 전체 사용자 목록을 조회한다.
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userQueryService.getAllUsers();
    }

    // 내 사용자 정보를 조회한다.
    @Transactional(readOnly = true)
    public UserResponse getMyInfo(String username) {
        return userQueryService.getMyInfo(username);
    }

    // 비밀번호를 변경하고 모든 refresh token을 폐기한다.
    public void changePassword(String username, String currentPassword, String newPassword) {
        passwordService.changePassword(username, currentPassword, newPassword);
        refreshTokenService.revokeAllByUsername(username, "PASSWORD_CHANGED");
        authEventLogService.passwordChange(username);
    }

    // 인증 코드를 검증하고 비밀번호를 재설정한다.
    public void resetPassword(String username, String email, String verificationCode, String newPassword) {
        emailCodeService.verifyCode(
                new VerifyCodeRequest(email, verificationCode, EmailCodePurpose.RESET_PASSWORD),
                true
        );
        passwordService.resetPassword(username, email, newPassword);
        refreshTokenService.revokeAllByUsername(username, "PASSWORD_RESET");
        authEventLogService.passwordReset(username);
    }

    // 인증 코드를 검증하고 사용자명을 조회한다.
    @Transactional(readOnly = true)
    public String findUsername(FindUsernameRequest request) {
        emailCodeService.verifyCode(
                new VerifyCodeRequest(request.email(), request.verificationCode(), EmailCodePurpose.FIND_USERNAME),
                true
        );
        return userQueryService.findUsername(request.email());
    }
}
