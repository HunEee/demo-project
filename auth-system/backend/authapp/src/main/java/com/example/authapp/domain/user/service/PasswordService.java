package com.example.authapp.domain.user.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.authapp.domain.audit.service.AuthEventLogService;
import com.example.authapp.domain.jwt.service.JwtService;
import com.example.authapp.domain.user.dto.UserRequestDTO;
import com.example.authapp.domain.user.entity.UserEntity;
import com.example.authapp.domain.user.exception.UserException;
import com.example.authapp.domain.user.repository.UserRepository;
import com.example.authapp.domain.verificatoin.service.EmailCodeService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class PasswordService {
	
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    private final JwtService jwtService;
    private final EmailCodeService emailCodeService;
    private final AuthEventLogService authEventLogService;
	

    // 비밀번호 변경(로그인 상태)
    public void changePassword(String username, String currentPassword, String newPassword, String ip, String device) {
        // 1. 유저 조회
        UserEntity user = userRepository.findByUsername(username).orElseThrow(UserException::userNotFound);

        // 2. 현재 비밀번호 검증
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호 불일치");
        }

        // 3. 새 비밀번호 검증 (간단 버전)
        if (currentPassword.equals(newPassword)) {
            throw new IllegalArgumentException("기존 비밀번호와 동일");
        }

        // 4. 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(newPassword);

        // 5. 변경
        user.changePassword(encodedPassword);

        // 6. 모든 Refresh Token 무효화 (강제 로그아웃)
        jwtService.revokeAllByUsername(username);

        // 7. 보안 로그 기록
        authEventLogService.passwordChange(username,ip,device);
    }
    
    // 비밀번호 초기화(비밀번호 찾기)
    public void resetPassword(String username, String email, String verifyCode, String newPassword, String ip,String device) {
        // 1. 이메일 인증코드 검증
        emailCodeService.verifyResetPasswordCode(email, verifyCode);
        
        // 2. 유저 조회
        UserEntity user = userRepository.findByUsernameAndEmail(username, email)
        		.orElseThrow(UserException::userNotFound);
        
        // 3. 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(newPassword);

        // 4. 변경
        user.changePassword(encodedPassword);

        // 5. 기존 Refresh Token 전체 만료
        jwtService.revokeAllByUsername(user.getUsername());
        
        // 6. 로그 기록
        authEventLogService.passwordReset(username,ip,device);
    }
    
    
	
}
