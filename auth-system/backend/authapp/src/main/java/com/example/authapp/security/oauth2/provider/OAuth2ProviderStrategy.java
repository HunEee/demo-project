package com.example.authapp.security.oauth2.provider;

import java.util.Map;

//공급자 전략 인터페이스
public interface OAuth2ProviderStrategy {

    // google / naver / kakao
    String provider();

    // 공급자 응답 파싱
    OAuth2UserInfo extract(
            Map<String, Object> attributes
    );
    
}
