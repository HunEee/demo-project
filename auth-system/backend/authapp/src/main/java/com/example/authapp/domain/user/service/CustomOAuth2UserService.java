package com.example.authapp.domain.user.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.authapp.domain.audit.service.AuthEventLogService;
import com.example.authapp.domain.user.entity.RoleEntity;
import com.example.authapp.domain.user.entity.SocialProviderType;
import com.example.authapp.domain.user.entity.UserEntity;
import com.example.authapp.domain.user.oauth.CustomOAuth2User;
import com.example.authapp.domain.user.oauth.OAuth2StrategyFactory;
import com.example.authapp.domain.user.oauth.OAuth2UserInfo;
import com.example.authapp.domain.user.repository.RoleRepository;
import com.example.authapp.domain.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final OAuth2StrategyFactory strategyFactory;
    
    private final AuthEventLogService authEventLogService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {

    	// 부모 메소드 호출
        OAuth2User oauthUser = super.loadUser(request);

        // provider 제공자별 데이터 획득 
        String registrationId = request.getClientRegistration().getRegistrationId().toUpperCase();
        OAuth2UserInfo info = strategyFactory.get(registrationId).extract(oauthUser.getAttributes());
        String username = info.username();
        Optional <UserEntity> optional = userRepository.findByUsername(username);
        UserEntity user;

        // 데이터베이스 조회 -> 존재하면 업데이트, 없으면 신규 가입
        if (optional.isPresent()) {
            user = optional.get();
            // 기존 유저 업데이트
            user.updateOAuthProfile(info.email(),info.nickname());
        } else {
        	// 기본 ROLE_USER 조회
            RoleEntity role = roleRepository.findByName("ROLE_USER").orElseThrow();
            // 신규 유저 추가
            user = UserEntity.builder()
                    .username(username)
                    .password("")
                    .email(info.email())
                    .nickname(info.nickname())
                    .enabled(true)
                    .locked(false)
                    .isSocial(true)
                    .providerId(info.providerId())
                    .socialProviderType(SocialProviderType.valueOf(info.provider()))
                    .build();

            user.addRole(role);

            userRepository.save(user);

            authEventLogService.signupOauth2Success(username,info.provider());
        }

        List<GrantedAuthority> authorities =
                user.getRoles().stream()
                        .map(role -> new SimpleGrantedAuthority(role.getName()))
                        .collect(Collectors.toList());

        return new CustomOAuth2User(oauthUser.getAttributes(), authorities, username);
        
    }
    
    
}