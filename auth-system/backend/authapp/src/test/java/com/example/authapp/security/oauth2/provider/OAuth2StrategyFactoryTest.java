package com.example.authapp.security.oauth2.provider;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class OAuth2StrategyFactoryTest {

    @Test
    void supportsNaverGoogleKakaoProviders() {
        OAuth2StrategyFactory factory = new OAuth2StrategyFactory(List.of(
                new NaverOAuth2Strategy(),
                new GoogleOAuth2Strategy(),
                new KakaoOAuth2Strategy()
        ));

        assertThat(factory.get("naver")).isInstanceOf(NaverOAuth2Strategy.class);
        assertThat(factory.get("google")).isInstanceOf(GoogleOAuth2Strategy.class);
        assertThat(factory.get("kakao")).isInstanceOf(KakaoOAuth2Strategy.class);
    }
}
