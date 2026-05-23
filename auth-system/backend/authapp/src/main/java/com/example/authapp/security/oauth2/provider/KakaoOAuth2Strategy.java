package com.example.authapp.security.oauth2.provider;

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

        String providerId = OAuth2AttributeValidator.requiredString(attributes, "id");

        Map<String, Object> account = OAuth2AttributeValidator.requiredMap(attributes, "kakao_account");
        OAuth2AttributeValidator.requireTrue(account, "has_email");
        OAuth2AttributeValidator.requireTrue(account, "is_email_valid");
        OAuth2AttributeValidator.requireTrue(account, "is_email_verified");

        String email = OAuth2AttributeValidator.requiredString(account, "email");
        String nickname = resolveNickname(account, email, providerId);

        return new OAuth2UserInfo("KAKAO", providerId, email, nickname);
    
    }

    private String resolveNickname(Map<String, Object> account, String email, String providerId) {
        Map<String, Object> profile = OAuth2AttributeValidator.optionalMap(account, "profile");
        if (profile != null) {
            String nickname = OAuth2AttributeValidator.optionalString(profile, "nickname");
            if (nickname != null) {
                return nickname;
            }
        }

        int atIndex = email.indexOf('@');
        if (atIndex > 0) {
            return email.substring(0, atIndex);
        }

        return "kakao_" + providerId;
    }
    
}
