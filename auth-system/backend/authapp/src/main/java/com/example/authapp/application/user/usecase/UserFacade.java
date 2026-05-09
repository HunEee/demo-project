package com.example.authapp.application.user.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.authapp.application.user.dto.FindUsernameRequest;
import com.example.authapp.application.user.dto.SignupRequest;
import com.example.authapp.application.user.dto.UpdateUserProfileRequest;
import com.example.authapp.domain.jwt.service.RefreshTokenService;
import com.example.authapp.domain.user.service.UserCommandService;
import com.example.authapp.domain.user.service.UserQueryService;
import com.example.authapp.domain.verificatoin.dto.VerifyCodeRequest;
import com.example.authapp.domain.verificatoin.entity.EmailCodePurpose;
import com.example.authapp.domain.verificatoin.service.EmailCodeService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UserFacade {
	
    private final EmailCodeService emailCodeService;
    private final RefreshTokenService refreshTokenService;

    // user 서비스
    private final UserCommandService userCommandService;
    private final UserQueryService userQueryService;
    
    
    // 회원가입
    public Long signup(SignupRequest dto) {
        // 인증코드 검증
        emailCodeService.verifyCode(new VerifyCodeRequest(dto.email(), dto.verificationCode(), EmailCodePurpose.SIGNUP),true);
        // 회원 생성
        return userCommandService.createUser(dto);
    }
    
    // 내 정보 수정
    public Long updateMyProfile(String username, UpdateUserProfileRequest dto) {
        return userCommandService.updateUser(username,dto);
    }

    // 회원 탈퇴
    public void deleteMyAccount(String username) {
        // 회원 비활성화
        userCommandService.deleteMyAccount(username);
        // 보안 정합성 유지
        refreshTokenService.removeRefreshUser(username);
    }

    
    // username 찾기
    @Transactional(readOnly = true)
    public String findUsername(FindUsernameRequest dto) {
        // 인증코드 검증
        emailCodeService.verifyCode(new VerifyCodeRequest(dto.email(), dto.verificationCode(), EmailCodePurpose.FIND_USERNAME), true);
        // username 반환
        return userQueryService.findUsername(dto);
    }

    
    
    
	

}
