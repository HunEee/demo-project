package com.example.authapp.application.user.usecase;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.authapp.application.user.dto.SignupRequest;
import com.example.authapp.application.user.dto.UpdateUserProfileRequest;
import com.example.authapp.domain.authorization.entity.RoleEntity;
import com.example.authapp.domain.authorization.repository.RoleRepository;
import com.example.authapp.domain.jwt.service.RefreshTokenService;
import com.example.authapp.domain.user.entity.UserEntity;
import com.example.authapp.domain.user.event.UserDeletedEvent;
import com.example.authapp.domain.user.event.UserProfileUpdatedEvent;
import com.example.authapp.domain.user.event.UserSignedUpEvent;
import com.example.authapp.domain.user.exception.UserException;
import com.example.authapp.domain.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UserCommandService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenService refreshTokenService;
    private final ApplicationEventPublisher eventPublisher;

    // 일반 회원가입을 처리한다.
    public Long createUser(SignupRequest dto) {
        if (userRepository.existsByUsername(dto.username())) {
            throw UserException.usernameAlreadyExists();
        }
        if (userRepository.existsByEmail(dto.email())) {
            throw UserException.emailAlreadyExists();
        }

        RoleEntity userRole = roleRepository.findByName("ROLE_USER").orElseThrow(UserException::roleNotFound);
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

        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            throw UserException.emailAlreadyExists();
        }

        eventPublisher.publishEvent(new UserSignedUpEvent(user.getId(), user.getUsername(), user.getEmail()));
        return user.getId();
    }

    // 내 회원 정보를 수정한다.
    public Long updateUser(String sessionUsername, UpdateUserProfileRequest dto) throws AccessDeniedException {
        UserEntity entity = userRepository.findByUsernameAndLocked(sessionUsername, false)
                .orElseThrow(UserException::userNotFound);
        entity.updateProfile(dto.nickname(), dto.profileImage());
        eventPublisher.publishEvent(new UserProfileUpdatedEvent(sessionUsername));
        return entity.getId();
    }

    // 내 계정을 탈퇴 처리한다.
    public void deleteMyAccount(String username) {
        deleteAccount(username);
    }

    // 관리자가 특정 사용자 계정을 탈퇴 처리한다.
    public void deleteUser(String username) throws AccessDeniedException {
        deleteAccount(username);
    }

    // 사용자 계정을 비활성화하고 refresh token을 폐기한다.
    private void deleteAccount(String username) {
        UserEntity user = userRepository.findByUsernameAndDeletedAtIsNull(username)
                .orElseThrow(UserException::alreadyDeleted);
        if (user.isDeleted()) {
            throw UserException.alreadyDeleted();
        }

        user.deactivate();
        refreshTokenService.revokeAllByUsername(username, "ACCOUNT_DELETED");
        eventPublisher.publishEvent(new UserDeletedEvent(username));
    }
}
