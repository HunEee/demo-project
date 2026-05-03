package com.example.authapp.domain.user.service;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.authapp.domain.audit.service.AuthEventLogService;
import com.example.authapp.domain.jwt.service.JwtService;
import com.example.authapp.domain.user.dto.UserRequestDTO;
import com.example.authapp.domain.user.entity.RoleEntity;
import com.example.authapp.domain.user.entity.UserEntity;
import com.example.authapp.domain.user.entity.UserRoleType;
import com.example.authapp.domain.user.exception.UserException;
import com.example.authapp.domain.user.repository.RoleRepository;
import com.example.authapp.domain.user.repository.UserRepository;
import com.example.authapp.domain.verificatoin.service.EmailCodeService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UserCommandService {

	private final PasswordEncoder passwordEncoder;
	private final UserRepository userRepository;
    private final RoleRepository roleRepository;
	private final JwtService jwtService;
    private final AuthEventLogService securityEventService;
    private final EmailCodeService emailCodeService;
    
    
    // 자체 로그인 회원 가입
    public Long addUser(UserRequestDTO dto) {

        // 1. 이메일 인증코드 검증
        emailCodeService.verifySignupCode(dto.getEmail(),dto.getVerificationCode());
    	
        // =========================
        // 2. 중복 체크
    	if (userRepository.existsByUsername(dto.getUsername())) {
    	    throw UserException.duplicateUsername();
    	}

    	if (userRepository.existsByEmail(dto.getEmail())) {
    	    throw UserException.duplicateEmail();
    	}

        // =========================
        // 3. 권한 조회
    	RoleEntity userRole = roleRepository.findByName("ROLE_USER")
    	        .orElseThrow(UserException::roleNotFound);
        
        // =========================
        // 4. 회원 생성
        UserEntity user = UserEntity.builder()
                .username(dto.getUsername())
                .password(passwordEncoder.encode(dto.getPassword()))
                .email(dto.getEmail())
                .nickname(dto.getNickname())
                .profileImage(dto.getProfileImage())
                .locked(false)
                .enabled(true)
                .isSocial(false)
                .socialProviderType(null)
                .providerId(null)
                .build();
        
        user.addRole(userRole);
        
        return userRepository.save(user).getId();
    }    
    
    // 자체 로그인 회원 정보 수정
    public Long updateUser(UserRequestDTO dto) throws AccessDeniedException {

        // 본인만 수정 가능 검증
        String sessionUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!sessionUsername.equals(dto.getUsername())) {
            throw new AccessDeniedException("본인 계정만 수정 가능");
        }
        
        // 중복 이메일 검증
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("이미 이메일이 존재합니다.");
        }
        
        // 조회
        UserEntity entity = userRepository.findByUsernameAndLockedAndIsSocial(dto.getUsername(), false, false)
                							.orElseThrow(() -> new UsernameNotFoundException(dto.getUsername()));

        // 회원 정보 수정
        entity.updateUser(dto);

        return userRepository.save(entity).getId();
    }
    
    // 회원 탈퇴
    public void deleteUser(UserRequestDTO dto) throws AccessDeniedException {

        // 본인 및 어드민만 삭제 가능 검증
        SecurityContext context = SecurityContextHolder.getContext();
        String sessionUsername = context.getAuthentication().getName();
        String sessionRole = context.getAuthentication().getAuthorities().iterator().next().getAuthority();

        boolean isOwner = sessionUsername.equals(dto.getUsername());
        boolean isAdmin = sessionRole.equals("ROLE_"+UserRoleType.ROLE_ADMIN.name());

        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("본인 혹은 관리자만 삭제할 수 있습니다.");
        }

        // 유저 제거
        userRepository.deleteByUsername(dto.getUsername());

        // Refresh 토큰 제거
        jwtService.removeRefreshUser(dto.getUsername());
    }
    
    

    
}
    

