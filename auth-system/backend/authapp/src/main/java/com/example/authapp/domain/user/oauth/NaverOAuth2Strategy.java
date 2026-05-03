package com.example.authapp.domain.user.oauth;

import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class NaverOAuth2Strategy
        implements OAuth2ProviderStrategy {

    @Override
    public String provider() {
        return "NAVER";
    }

    @Override
    @SuppressWarnings("unchecked")
    public OAuth2UserInfo extract(Map<String, Object> attributes) {

        Map<String, Object> response = (Map<String, Object>) attributes.get("response");

        String providerId = response.get("id").toString();
        String email = response.get("email").toString();
        String nickname = response.get("nickname").toString();

        return new OAuth2UserInfo("NAVER", providerId, email, nickname);
        
    }
    
}