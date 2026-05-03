package com.example.authapp.domain.user.oauth;

import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class GoogleOAuth2Strategy implements OAuth2ProviderStrategy {

    @Override
    public String provider() {
        return "GOOGLE";
    }

    @Override
    public OAuth2UserInfo extract(Map<String, Object> attributes) {

        String providerId = attributes.get("sub").toString();
        String email = attributes.get("email").toString();
        String nickname = attributes.get("name").toString();
        
        return new OAuth2UserInfo("GOOGLE", providerId, email, nickname);
    
    }
    
    
}