package com.example.authapp.domain.user.oauth;

import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class KakaoOAuth2Strategy
        implements OAuth2ProviderStrategy {

    @Override
    public String provider() {
        return "KAKAO";
    }

    @Override
    @SuppressWarnings("unchecked")
    public OAuth2UserInfo extract(Map<String, Object> attributes) {

        String providerId = attributes.get("id").toString();

        Map<String, Object> account = (Map<String, Object>) attributes.get("kakao_account");
        Map<String, Object> profile = (Map<String, Object>) account.get("profile");

        String email = account.get("email").toString();
        String nickname = profile.get("nickname").toString();

        return new OAuth2UserInfo("KAKAO", providerId, email, nickname);
    
    }
    
}