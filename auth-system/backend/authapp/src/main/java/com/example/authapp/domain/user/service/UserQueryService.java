package com.example.authapp.domain.user.service;

import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.authapp.domain.user.dto.UserResponse;
import com.example.authapp.domain.user.entity.UserEntity;
import com.example.authapp.domain.user.exception.UserException;
import com.example.authapp.domain.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserQueryService {

    private final UserRepository userRepository;
    
    // ==================================================================================================================
    // 존재 여부
    // ==================================================================================================================
    
    // 자체 로그인 회원 가입 (존재 여부)
    public boolean existUsername(String username) {
        return userRepository.existsByUsername(username);
    }
    
    // 이메일 존재 여부(회원가입, 이메일 코드 발송)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
    
    // ==================================================================================================================
    // 단건 조회
    // ==================================================================================================================

    public UserEntity getByUsername(String username) {
        return userRepository.findByUsernameAndDeletedAtIsNull(username).orElseThrow(UserException::userNotFound);
    }
        
    // 이메일로 username 찾기
    public UserEntity getByEmail(String email) {
        return userRepository.findByEmailAndDeletedAtIsNull(email).orElseThrow(UserException::userNotFound);
    }

    
    // 이메일과 username로 패스워드 찾기
    public UserEntity getByUsernameAndEmail(String username, String email) {
        return userRepository.findByUsernameAndEmailAndDeletedAtIsNull(username, email).orElseThrow(UserException::userNotFound);
    }
    
    // ==================================================================================================================
    // 목록 조회
    // ==================================================================================================================
    // 관리자용 전체 유저 조회 
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .filter(user -> user.getDeletedAt() == null) // soft delete 제외
                .map(user -> new UserResponse(
                        user.getUsername(),
                        user.getIsSocial(),
                        user.getNickname(),
                        user.getEmail()
                ))
                .toList();
    }
    
    // ==================================================================================================================
    // 내 정보 조회 (Controller에서 username 전달)
    // ==================================================================================================================

    public UserResponse getMyInfo(String username) {
        var user = userRepository.findByUsername(username).orElseThrow();
        return new UserResponse(user.getUsername(),user.getIsSocial(),user.getNickname(),user.getEmail());
    }
    
    
    // 자체/소셜 유저 정보 조회
    public UserResponse readUser() {
        
    	Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        // 인증 여부 먼저 확인
        if (authentication == null || !authentication.isAuthenticated()|| authentication instanceof AnonymousAuthenticationToken) {
            throw new AccessDeniedException("로그인이 필요합니다.");
        }

        String username = authentication.getName();
        
        // 유저 존재 여부 확인
        UserEntity entity = userRepository.findByUsernameAndLocked(username, false)
                .orElseThrow(() -> new UsernameNotFoundException("해당 유저를 찾을 수 없습니다: " + username));

        return new UserResponse(username, entity.getIsSocial(), entity.getNickname(), entity.getEmail());
    }

    
}
