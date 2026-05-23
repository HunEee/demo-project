package com.example.authapp.security.oauth2.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.authapp.domain.user.entity.UserEntity;
import com.example.authapp.domain.user.service.SocialUserService;
import com.example.authapp.security.oauth2.principal.CustomOAuth2User;
import com.example.authapp.security.oauth2.provider.OAuth2StrategyFactory;
import com.example.authapp.security.oauth2.provider.OAuth2UserInfo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final SocialUserService socialUserService;
    private final OAuth2StrategyFactory strategyFactory;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {

        // 부모 메소드 호출
        OAuth2User oauthUser = super.loadUser(request);

        // provider 제공자별 데이터 추출
        String registrationId = request.getClientRegistration().getRegistrationId().toUpperCase();
        OAuth2UserInfo info = strategyFactory.get(registrationId).extract(oauthUser.getAttributes());
        UserEntity user = socialUserService.loadOrCreate(info);

        List<GrantedAuthority> authorities =
                user.getRoles().stream()
                        .map(role -> new SimpleGrantedAuthority(role.getName()))
                        .collect(Collectors.toList());

        return new CustomOAuth2User(oauthUser.getAttributes(), authorities, user.getUsername());
    }
}
