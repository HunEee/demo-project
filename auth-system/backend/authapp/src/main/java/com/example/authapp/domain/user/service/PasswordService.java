package com.example.authapp.domain.user.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.authapp.domain.user.entity.UserEntity;
import com.example.authapp.domain.user.exception.UserException;
import com.example.authapp.domain.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class PasswordService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public void changePassword(String username, String currentPassword, String newPassword) {
        // 탈퇴하지 않은 사용자를 조회한다.
        UserEntity user = userRepository.findByUsernameAndDeletedAtIsNull(username)
                .orElseThrow(UserException::userNotFound);

        // 현재 비밀번호가 맞는지 검증한다.
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }

        // 새 비밀번호가 기존 비밀번호와 다른지 검증한다.
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new IllegalArgumentException("기존 비밀번호와 동일합니다.");
        }

        // 새 비밀번호를 암호화해서 저장한다.
        user.changePassword(passwordEncoder.encode(newPassword));
    }

    public void resetPassword(String username, String email, String newPassword) {
        // 사용자명과 이메일이 일치하는 사용자를 조회한다.
        UserEntity user = userRepository.findByUsernameAndEmailAndDeletedAtIsNull(username, email)
                .orElseThrow(UserException::userNotFound);

        // 새 비밀번호를 암호화해서 저장한다.
        user.changePassword(passwordEncoder.encode(newPassword));
    }
}
