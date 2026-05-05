package com.example.authapp.domain.user.service;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.authapp.domain.audit.service.AuthEventLogService;
import com.example.authapp.domain.jwt.service.RefreshTokenService;
import com.example.authapp.domain.user.dto.user.FindUsernameRequest;
import com.example.authapp.domain.user.dto.user.SignupRequest;
import com.example.authapp.domain.user.dto.user.UpdateUserRequest;
import com.example.authapp.domain.user.entity.RoleEntity;
import com.example.authapp.domain.user.entity.UserEntity;
import com.example.authapp.domain.user.entity.UserRoleType;
import com.example.authapp.domain.user.exception.UserException;
import com.example.authapp.domain.user.repository.RoleRepository;
import com.example.authapp.domain.user.repository.UserRepository;
import com.example.authapp.domain.verificatoin.dto.VerifyCodeRequest;
import com.example.authapp.domain.verificatoin.entity.EmailCodePurpose;
import com.example.authapp.domain.verificatoin.service.EmailCodeService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UserCommandService {

	private final PasswordEncoder passwordEncoder;
	private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    
    private final RefreshTokenService refreshTokenService;
    private final AuthEventLogService authEventLogService;
    private final EmailCodeService emailCodeService;
    
    
    // 자체 로그인 회원 가입
    public Long addUser(SignupRequest dto) {
        // 1. 이메일 인증코드 검증
        emailCodeService.verifyCode(new VerifyCodeRequest(dto.email(),dto.verificationCode(),EmailCodePurpose.SIGNUP),true);

        // 2. 중복 체크
    	if (userRepository.existsByUsername(dto.username())) throw UserException.usernameAlreadyExists();    	
    	if (userRepository.existsByEmail(dto.email())) throw UserException.emailAlreadyExists();
    	
        // 3. 권한 조회
    	RoleEntity userRole = roleRepository.findByName("ROLE_USER").orElseThrow(UserException::roleNotFound);
        
        // 4. 회원 생성
        UserEntity user = UserEntity.builder()
                .username(dto.username())
                .password(passwordEncoder.encode(dto.password()))
                .email(dto.email())
                .nickname(dto.nickname())
                .profileImage(dto.profileImage())
                .locked(false)
                .enabled(true)
                .isSocial(false)
                .socialProviderType(null)
                .providerId(null)
                .build();

        user.addRole(userRole);
        
        // 5. 저장
        userRepository.save(user);
        
        // 6. 이벤트 기록
        authEventLogService.signupSuccess(user.getUsername());
        
        return user.getId();

    }    
    
    // 내 회원 정보 수정
    public Long updateUser(String sessionUsername, UpdateUserRequest dto) throws AccessDeniedException {
        // 조회
        UserEntity entity = userRepository.findByUsernameAndLocked(sessionUsername, false).orElseThrow(UserException::userNotFound);

        // 회원정보 수정
        entity.updateProfile(dto.nickname(), dto.profileImage());

        // 로그 기록
        authEventLogService.accountProfileUpdated(sessionUsername);
        
        return entity.getId();
    }
    
    // 내 계정 삭제
    public void deleteMyAccount(String username) {
    	// 탈퇴할 유저를 가져옴
        UserEntity user = userRepository.findByUsernameAndDeletedAtIsNull(username).orElseThrow(UserException::alreadyDeleted);

        // 이미 탈퇴한 경우 방어
        if (user.isDeleted()) throw UserException.alreadyDeleted();

        // Soft delete
        user.deactivate();

        // 토큰 제거
        refreshTokenService.removeRefreshUser(username);

        // 로그 기록
        authEventLogService.accountDeactivated(username);
    }
    
    // ==================================================================================================================
    // username 찾기
    // ==================================================================================================================
    
    // 관리자 특정 회원 탈퇴 
    public void deleteUser(String targetUsername, String role) throws AccessDeniedException {
      
        boolean isAdmin = role.equals("ROLE_"+UserRoleType.ROLE_ADMIN.name());

        if (!isAdmin) {
            throw new AccessDeniedException("관리자만 삭제할 수 있습니다.");
        }

        UserEntity user = userRepository
                .findByUsernameAndDeletedAtIsNull(targetUsername)
                .orElseThrow(UserException::userNotFound);

        // Soft Delete로 통일
        user.deactivate();

        refreshTokenService.removeRefreshUser(targetUsername);

        authEventLogService.accountDeactivated(targetUsername);
    }
    
    // ==================================================================================================================
    // username 찾기
    // ==================================================================================================================
    
    public String findUsername(FindUsernameRequest dto) {
        // 1. 인증코드 검증
        emailCodeService.verifyCode(new VerifyCodeRequest(dto.email(), dto.verificationCode(), EmailCodePurpose.FIND_USERNAME),true);
        // 2. 조회
        UserEntity user = userRepository.findByEmailAndDeletedAtIsNull(dto.email()).orElseThrow(UserException::userNotFound);
        
        return user.getUsername();
    }

    
}
    

