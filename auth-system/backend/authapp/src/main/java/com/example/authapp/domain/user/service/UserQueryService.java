package com.example.authapp.domain.user.service;

import java.util.List;

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

    // 사용자명 존재 여부를 조회한다.
    public boolean existUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    // 이메일 존재 여부를 조회한다.
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    // 사용자 id로 사용자를 조회한다.
    public UserEntity getUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(UserException::userNotFound);
    }

    // 사용자명으로 삭제되지 않은 사용자를 조회한다.
    public UserEntity getByUsername(String username) {
        return userRepository.findByUsernameAndDeletedAtIsNull(username).orElseThrow(UserException::userNotFound);
    }

    // 이메일로 삭제되지 않은 사용자를 조회한다.
    public UserEntity getByEmail(String email) {
        return userRepository.findByEmailAndDeletedAtIsNull(email).orElseThrow(UserException::userNotFound);
    }

    // 사용자명과 이메일로 삭제되지 않은 사용자를 조회한다.
    public UserEntity getByUsernameAndEmail(String username, String email) {
        return userRepository.findByUsernameAndEmailAndDeletedAtIsNull(username, email).orElseThrow(UserException::userNotFound);
    }

    // 내 사용자 정보를 조회한다.
    public UserResponse getMyInfo(String username) {
        var user = userRepository.findByUsernameAndLocked(username, false).orElseThrow(UserException::userNotFound);
        return new UserResponse(user.getUsername(), user.isSocial(), user.getNickname(), user.getEmail());
    }

    // 이메일로 사용자명을 조회한다.
    public String findUsername(String email) {
        UserEntity user = userRepository.findByEmailAndDeletedAtIsNull(email).orElseThrow(UserException::userNotFound);
        if (user.isSocial() && !user.hasPasswordLogin()) {
            return "소셜 로그인 계정입니다.";
        }
        return user.getUsername();
    }

    // 삭제되지 않은 전체 사용자 목록을 조회한다.
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .filter(user -> user.getDeletedAt() == null)
                .map(user -> new UserResponse(
                        user.getUsername(),
                        user.isSocial(),
                        user.getNickname(),
                        user.getEmail()
                ))
                .toList();
    }
}
