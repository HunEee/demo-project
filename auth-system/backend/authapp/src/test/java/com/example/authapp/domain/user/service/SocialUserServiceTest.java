package com.example.authapp.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.authapp.domain.audit.service.AuthEventLogService;
import com.example.authapp.domain.authorization.entity.RoleEntity;
import com.example.authapp.domain.user.entity.SocialProviderType;
import com.example.authapp.domain.user.entity.UserEntity;
import com.example.authapp.domain.user.exception.UserException;
import com.example.authapp.domain.authorization.repository.RoleRepository;
import com.example.authapp.domain.user.repository.UserRepository;
import com.example.authapp.security.oauth2.provider.OAuth2UserInfo;

@ExtendWith(MockitoExtension.class)
class SocialUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private AuthEventLogService authEventLogService;

    @InjectMocks
    private SocialUserService socialUserService;

    @Test
    void createsSocialUserWhenProviderUserDoesNotExist() {
        OAuth2UserInfo info = new OAuth2UserInfo("KAKAO", "12345", "kakao@example.com", "kakaoUser");
        RoleEntity role = RoleEntity.builder().name("ROLE_USER").build();

        when(userRepository.findBySocialProviderTypeAndProviderId(SocialProviderType.KAKAO, info.providerId()))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmailAndDeletedAtIsNull(info.email())).thenReturn(Optional.empty());
        when(userRepository.findByUsername(info.username())).thenReturn(Optional.empty());
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(role));

        UserEntity user = socialUserService.loadOrCreate(info);

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(captor.capture());
        verify(authEventLogService).signupOauth2Success(info.username(), "KAKAO");
        assertThat(user).isSameAs(captor.getValue());
        assertThat(user.getUsername()).isEqualTo("KAKAO_12345");
        assertThat(user.isSocial()).isTrue();
        assertThat(user.getSocialProviderType()).isEqualTo(SocialProviderType.KAKAO);
    }

    @Test
    void rejectsLockedExistingSocialUser() {
        OAuth2UserInfo info = new OAuth2UserInfo("NAVER", "12345", "naver@example.com", "naverUser");
        UserEntity user = UserEntity.builder()
                .username(info.username())
                .password("")
                .email(info.email())
                .nickname(info.nickname())
                .enabled(true)
                .locked(true)
                .social(true)
                .providerId(info.providerId())
                .socialProviderType(SocialProviderType.NAVER)
                .build();

        when(userRepository.findBySocialProviderTypeAndProviderId(SocialProviderType.NAVER, info.providerId()))
                .thenReturn(Optional.of(user));

        assertThatThrownBy(() -> socialUserService.loadOrCreate(info))
                .isInstanceOf(UserException.class);
    }

    @Test
    void rejectsProviderMismatchForExistingUsername() {
        OAuth2UserInfo info = new OAuth2UserInfo("GOOGLE", "12345", "google@example.com", "googleUser");
        UserEntity user = UserEntity.builder()
                .username(info.username())
                .password("")
                .email(info.email())
                .nickname(info.nickname())
                .enabled(true)
                .locked(false)
                .social(true)
                .providerId("other")
                .socialProviderType(SocialProviderType.GOOGLE)
                .build();

        when(userRepository.findBySocialProviderTypeAndProviderId(SocialProviderType.GOOGLE, info.providerId()))
                .thenReturn(Optional.of(user));

        assertThatThrownBy(() -> socialUserService.loadOrCreate(info))
                .isInstanceOf(UserException.class);
    }

    @Test
    void linksSocialAccountWhenEmailAlreadyBelongsToLocalUser() {
        OAuth2UserInfo info = new OAuth2UserInfo("GOOGLE", "12345", "same@example.com", "googleUser");
        UserEntity existingUser = UserEntity.builder()
                .username("localUser")
                .password("encoded")
                .email(info.email())
                .nickname("local")
                .enabled(true)
                .locked(false)
                .social(false)
                .build();

        when(userRepository.findBySocialProviderTypeAndProviderId(SocialProviderType.GOOGLE, info.providerId()))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmailAndDeletedAtIsNull(info.email())).thenReturn(Optional.of(existingUser));

        UserEntity user = socialUserService.loadOrCreate(info);

        assertThat(user).isSameAs(existingUser);
        assertThat(user.getUsername()).isEqualTo("localUser");
        assertThat(user.getPassword()).isEqualTo("encoded");
        assertThat(user.isSocial()).isTrue();
        assertThat(user.getSocialProviderType()).isEqualTo(SocialProviderType.GOOGLE);
        assertThat(user.getProviderId()).isEqualTo("12345");
    }

    @Test
    void keepsExistingSocialProfileWhenProviderLoginReturnsDifferentProfile() {
        OAuth2UserInfo info = new OAuth2UserInfo("GOOGLE", "12345", "new@example.com", "googleUser");
        UserEntity socialUser = UserEntity.builder()
                .username(info.username())
                .password("")
                .email("old@example.com")
                .nickname("old")
                .enabled(true)
                .locked(false)
                .social(true)
                .providerId(info.providerId())
                .socialProviderType(SocialProviderType.GOOGLE)
                .build();

        when(userRepository.findBySocialProviderTypeAndProviderId(SocialProviderType.GOOGLE, info.providerId()))
                .thenReturn(Optional.of(socialUser));

        UserEntity user = socialUserService.loadOrCreate(info);

        assertThat(user.getEmail()).isEqualTo("old@example.com");
        assertThat(user.getNickname()).isEqualTo("old");
    }
    
    
}
