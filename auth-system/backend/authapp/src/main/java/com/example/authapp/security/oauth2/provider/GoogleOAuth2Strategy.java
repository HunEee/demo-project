package com.example.authapp.security.oauth2.provider;

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

        OAuth2AttributeValidator.requireTrue(attributes, "email_verified");

        String providerId = OAuth2AttributeValidator.requiredString(attributes, "sub");
        String email = OAuth2AttributeValidator.requiredString(attributes, "email");
        String nickname = resolveNickname(attributes, email, providerId);
        
        return new OAuth2UserInfo("GOOGLE", providerId, email, nickname);
    
    }

    private String resolveNickname(Map<String, Object> attributes, String email, String providerId) {
        String name = OAuth2AttributeValidator.optionalString(attributes, "name");
        if (name != null) {
            return name;
        }

        int atIndex = email.indexOf('@');
        if (atIndex > 0) {
            return email.substring(0, atIndex);
        }

        return "google_" + providerId;
    }
    
    
}
