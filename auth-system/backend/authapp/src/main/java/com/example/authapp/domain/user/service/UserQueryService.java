package com.example.authapp.domain.user.service;

import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.authapp.domain.user.dto.UserRequestDTO;
import com.example.authapp.domain.user.dto.UserResponseDTO;
import com.example.authapp.domain.user.entity.UserEntity;
import com.example.authapp.domain.user.exception.UserException;
import com.example.authapp.domain.user.repository.UserRepository;
import com.example.authapp.domain.verificatoin.service.EmailCodeService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserQueryService {

    private final UserRepository userRepository;
    private final EmailCodeService emailCodeService;
	
    // 자체 로그인 회원 가입 (존재 여부)
    public Boolean existUser(UserRequestDTO dto) {
        return userRepository.existsByUsername(dto.getUsername());
    }
    
    // 관리자용 전체 유저 조회 
    public List<UserResponseDTO> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(user -> new UserResponseDTO(
                        user.getUsername(),
                        user.getIsSocial(),
                        user.getNickname(),
                        user.getEmail()
                ))
                .toList();
    }

    public UserResponseDTO getMyInfo(String username) {
        var user = userRepository.findByUsername(username).orElseThrow();
        return new UserResponseDTO(
                user.getUsername(),
                user.getIsSocial(),
                user.getNickname(),
                user.getEmail()
        );
    }
    
    
    // 자체/소셜 유저 정보 조회
    public UserResponseDTO readUser() {
        
    	Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        // 인증 여부 먼저 확인
        if (authentication == null || !authentication.isAuthenticated()|| authentication instanceof AnonymousAuthenticationToken) {
            throw new AccessDeniedException("로그인이 필요합니다.");
        }

        String username = authentication.getName();
        
        // 유저 존재 여부 확인
        UserEntity entity = userRepository.findByUsernameAndLocked(username, false)
                .orElseThrow(() -> new UsernameNotFoundException("해당 유저를 찾을 수 없습니다: " + username));

        return new UserResponseDTO(username, entity.getIsSocial(), entity.getNickname(), entity.getEmail());
    }
    
    
    // username 찾기
    public String findUsername(UserRequestDTO dto) {
    	
        emailCodeService.verifyFindUsernameCode(dto.getEmail(), dto.getVerificationCode());

        UserEntity user = userRepository.findByEmail(dto.getEmail()).orElseThrow(UserException::userNotFound);

        String username = user.getUsername();
        
        return username;
    }
    
}
