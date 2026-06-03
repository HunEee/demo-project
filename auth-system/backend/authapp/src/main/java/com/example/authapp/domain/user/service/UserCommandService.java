package com.example.authapp.domain.user.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.authapp.application.user.dto.SignupRequest;
import com.example.authapp.application.user.dto.UpdateUserProfileRequest;
import com.example.authapp.domain.authorization.entity.RoleEntity;
import com.example.authapp.domain.user.entity.UserEntity;
import com.example.authapp.domain.user.event.UserDeletedEvent;
import com.example.authapp.domain.user.event.UserProfileUpdatedEvent;
import com.example.authapp.domain.user.event.UserSignedUpEvent;
import com.example.authapp.domain.user.exception.UserException;
import com.example.authapp.domain.authorization.repository.RoleRepository;
import com.example.authapp.domain.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UserCommandService {

	private final PasswordEncoder passwordEncoder;
	private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    
    private final ApplicationEventPublisher eventPublisher;
    
    
    // 자체 로그인 회원 가입
    public Long createUser(SignupRequest dto) {
        // 1. 중복 체크
    	if (userRepository.existsByUsername(dto.username())) throw UserException.usernameAlreadyExists();    	
    	if (userRepository.existsByEmail(dto.email())) throw UserException.emailAlreadyExists();
    	
        // 2. 권한 조회
    	RoleEntity userRole = roleRepository.findByName("ROLE_USER").orElseThrow(UserException::roleNotFound);
        
        // 3. 회원 생성
        UserEntity user = UserEntity.builder()
                .username(dto.username())
                .password(passwordEncoder.encode(dto.password()))
                .email(dto.email())
                .nickname(dto.nickname())
                .profileImage(dto.profileImage())
                .locked(false)
                .enabled(true)
                .social(false)
                .socialProviderType(null)
                .providerId(null)
                .build();

        user.addRole(userRole);
        
        // 4. 저장
        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            throw UserException.emailAlreadyExists();
        }
        
        // 5. 이벤트 발행
        eventPublisher.publishEvent(new UserSignedUpEvent(user.getId(), user.getUsername(), user.getEmail()));
        
        return user.getId();

    }    
    
    // 내 회원 정보 수정
    public Long updateUser(String sessionUsername, UpdateUserProfileRequest dto) throws AccessDeniedException {
        // 조회
        UserEntity entity = userRepository.findByUsernameAndLocked(sessionUsername, false).orElseThrow(UserException::userNotFound);

        // 회원정보 수정
        entity.updateProfile(dto.nickname(), dto.profileImage());

        // 이벤트 발행
        eventPublisher.publishEvent(new UserProfileUpdatedEvent(sessionUsername));
        
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
        
        // 로그 기록 이벤트 발행
        eventPublisher.publishEvent(new UserDeletedEvent(username));
    }
    
    // ==================================================================================================================
    // 관리자 기능
    // ==================================================================================================================
    
    // 관리자 특정 회원 탈퇴 
    public void deleteUser(String username) throws AccessDeniedException {
        UserEntity user = userRepository
                .findByUsernameAndDeletedAtIsNull(username)
                .orElseThrow(UserException::alreadyDeleted);

        if (user.isDeleted()) throw UserException.alreadyDeleted();

        // Soft Delete로 통일
        user.deactivate();
        
        // 로그 기록 이벤트 발행
        eventPublisher.publishEvent(new UserDeletedEvent(username));
    }
    

    
}
    
