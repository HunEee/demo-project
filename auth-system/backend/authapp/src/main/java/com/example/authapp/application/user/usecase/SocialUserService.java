package com.example.authapp.application.user.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.authapp.domain.audit.service.AuthEventLogService;
import com.example.authapp.domain.authorization.entity.RoleEntity;
import com.example.authapp.domain.authorization.repository.RoleRepository;
import com.example.authapp.domain.user.entity.SocialProviderType;
import com.example.authapp.domain.user.entity.UserEntity;
import com.example.authapp.domain.user.exception.UserException;
import com.example.authapp.domain.user.repository.UserRepository;
import com.example.authapp.security.oauth2.provider.OAuth2UserInfo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class SocialUserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuthEventLogService authEventLogService;

    // 소셜 사용자를 조회하거나 새로 생성한다.
    public UserEntity loadOrCreate(OAuth2UserInfo info) {
        SocialProviderType providerType = SocialProviderType.valueOf(info.provider());
        return userRepository
                .findBySocialProviderTypeAndProviderId(providerType, info.providerId())
                .map(user -> validateAndReturnExistingSocialUser(user, info))
                .orElseGet(() -> loadByEmailOrCreate(providerType, info));
    }

    // 기존 소셜 계정을 검증하고 반환한다.
    private UserEntity validateAndReturnExistingSocialUser(UserEntity user, OAuth2UserInfo info) {
        validateAvailable(user);
        validateProvider(user, info);
        return user;
    }

    // 이메일 기준 기존 계정을 찾거나 새 소셜 계정을 생성한다.
    private UserEntity loadByEmailOrCreate(SocialProviderType providerType, OAuth2UserInfo info) {
        return userRepository
                .findByEmailAndDeletedAtIsNull(info.email())
                .map(user -> linkSocialAccount(user, providerType, info))
                .orElseGet(() -> createSocialUser(info));
    }

    // 기존 일반 계정에 소셜 계정을 연결한다.
    private UserEntity linkSocialAccount(UserEntity user, SocialProviderType providerType, OAuth2UserInfo info) {
        validateAvailable(user);
        validateSocialLinkAvailable(user, info);
        user.linkSocialAccount(providerType, info.providerId());
        return user;
    }

    // 새 소셜 회원을 생성한다.
    private UserEntity createSocialUser(OAuth2UserInfo info) {
        userRepository.findByUsername(info.username())
                .ifPresent(user -> {
                    throw UserException.usernameAlreadyExists();
                });

        RoleEntity role = roleRepository.findByName("ROLE_USER")
                .orElseThrow(UserException::roleNotFound);

        UserEntity user = UserEntity.builder()
                .username(info.username())
                .password("")
                .email(info.email())
                .nickname(info.nickname())
                .enabled(true)
                .locked(false)
                .social(true)
                .providerId(info.providerId())
                .socialProviderType(SocialProviderType.valueOf(info.provider()))
                .build();

        user.addRole(role);
        userRepository.save(user);
        authEventLogService.signupOauth2Success(info.username(), info.provider());
        return user;
    }

    // 계정 사용 가능 여부를 검증한다.
    private void validateAvailable(UserEntity user) {
        if (user.isDeleted() || user.isLocked() || !user.isEnabled()) {
            throw UserException.accountUnavailable();
        }
    }

    // 소셜 provider와 providerId가 일치하는지 검증한다.
    private void validateProvider(UserEntity user, OAuth2UserInfo info) {
        if (!user.isSocial()
                || user.getSocialProviderType() != SocialProviderType.valueOf(info.provider())
                || !info.providerId().equals(user.getProviderId())) {
            throw UserException.socialAccountConflict();
        }
    }

    // 기존 계정에 소셜 연결이 가능한지 검증한다.
    private void validateSocialLinkAvailable(UserEntity user, OAuth2UserInfo info) {
        if (!user.isSocial()) {
            return;
        }
        validateProvider(user, info);
    }
}
