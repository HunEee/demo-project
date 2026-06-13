package com.example.authapp.domain.user.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.authapp.domain.audit.service.AuthEventLogService;
import com.example.authapp.domain.jwt.service.RefreshTokenService;
import com.example.authapp.domain.user.entity.UserEntity;
import com.example.authapp.domain.user.exception.UserException;
import com.example.authapp.domain.user.repository.UserRepository;
import com.example.authapp.domain.verificatoin.dto.VerifyCodeRequest;
import com.example.authapp.domain.verificatoin.entity.EmailCodePurpose;
import com.example.authapp.domain.verificatoin.service.EmailCodeService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class PasswordService {
	
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    private final RefreshTokenService refreshTokenService;
    private final EmailCodeService emailCodeService;
    private final AuthEventLogService authEventLogService;
	

    // 비밀번호 변경(로그인 상태)
    public void changePassword(String username, String currentPassword, String newPassword) {
        // 1. 유저 조회(탈퇴/잠금 제외)
        UserEntity user = userRepository.findByUsernameAndDeletedAtIsNull(username).orElseThrow(UserException::userNotFound);

        // 2. 현재 비밀번호 검증
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호 불일치");
        }

        // 3. 새 비밀번호 검증 
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new IllegalArgumentException("기존 비밀번호와 동일");
        }

        // 4. 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(newPassword);

        // 5. 변경
        user.changePassword(encodedPassword);

        // 6. 모든 Refresh Token 무효화 (강제 로그아웃)
        refreshTokenService.revokeAllByUsername(username, "PASSWORD_CHANGED");

        // 7. 보안 로그 기록
        authEventLogService.passwordChange(username);
    }
    
    // 비밀번호 초기화(비밀번호 찾기)
    public void resetPassword(String username, String email, String verifyCode, String newPassword) {
        // 1. 인증코드 검증 
        emailCodeService.verifyCode(new VerifyCodeRequest(email, verifyCode, EmailCodePurpose.RESET_PASSWORD),true);
        // 2. 유저 조회
        UserEntity user = userRepository.findByUsernameAndEmailAndDeletedAtIsNull(username, email)
        		.orElseThrow(UserException::userNotFound);
        
        // 3. 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(newPassword);

        // 4. 변경
        user.changePassword(encodedPassword);

        // 5. 기존 Refresh Token 전체 만료
        refreshTokenService.revokeAllByUsername(user.getUsername(), "PASSWORD_RESET");
        
        // 6. 로그 기록
        authEventLogService.passwordReset(username);
    }
    
    
	
}
