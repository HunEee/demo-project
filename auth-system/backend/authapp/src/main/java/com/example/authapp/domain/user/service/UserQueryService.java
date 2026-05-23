package com.example.authapp.domain.user.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.authapp.application.user.dto.FindUsernameRequest;
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

    // 로그인 성공 반환
	public UserEntity getUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(UserException::userNotFound);
	}
    
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
    // 내 정보 조회 (Controller에서 username 전달)
    // ==================================================================================================================

    public UserResponse getMyInfo(String username) {
        var user = userRepository.findByUsernameAndLocked(username, false).orElseThrow(UserException::userNotFound);
        return new UserResponse(user.getUsername(),user.isSocial(),user.getNickname(),user.getEmail());
    }
    
    // ==================================================================================================================
    // username 찾기
    // ==================================================================================================================
    public String findUsername(FindUsernameRequest dto) {
        UserEntity user = userRepository.findByEmailAndDeletedAtIsNull(dto.email()).orElseThrow(UserException::userNotFound);
        if (user.isSocial() && !user.hasPasswordLogin()) {
            return "소셜 로그인 계정입니다.";
        }
        return user.getUsername();
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
                        user.isSocial(),
                        user.getNickname(),
                        user.getEmail()
                ))
                .toList();
    }



    
}
