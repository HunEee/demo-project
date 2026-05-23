package com.example.authapp.security.oauth2.provider;

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

        Map<String, Object> response = OAuth2AttributeValidator.requiredMap(attributes, "response");

        String providerId = OAuth2AttributeValidator.requiredString(response, "id");
        String email = OAuth2AttributeValidator.requiredString(response, "email");
        String nickname = resolveNickname(response, email, providerId);

        return new OAuth2UserInfo("NAVER", providerId, email, nickname);
        
    }

    //nickname -> name -> email 앞부분 -> naver_{providerId} 순서로 대체 닉네임을 만들도록 세팅
    private String resolveNickname(Map<String, Object> response, String email, String providerId) {
        String nickname = OAuth2AttributeValidator.optionalString(response, "nickname");
        if (nickname != null) {
            return nickname;
        }

        String name = OAuth2AttributeValidator.optionalString(response, "name");
        if (name != null) {
            return name;
        }

        int atIndex = email.indexOf('@');
        if (atIndex > 0) {
            return email.substring(0, atIndex);
        }

        return "naver_" + providerId;
    }
    
}
