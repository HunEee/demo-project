package com.example.authapp.security.oauth2.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.junit.jupiter.api.Test;

class OAuth2ProviderStrategyTest {

    @Test
    void naverUsesNameWhenNicknameIsMissing() {
        NaverOAuth2Strategy strategy = new NaverOAuth2Strategy();

        OAuth2UserInfo userInfo = strategy.extract(Map.of(
                "response", Map.of(
                        "id", "12345",
                        "email", "naver@example.com",
                        "name", "naverUser"
                )
        ));

        assertThat(userInfo.nickname()).isEqualTo("naverUser");
    }

    @Test
    void naverUsesEmailPrefixWhenNicknameAndNameAreMissing() {
        NaverOAuth2Strategy strategy = new NaverOAuth2Strategy();

        OAuth2UserInfo userInfo = strategy.extract(Map.of(
                "response", Map.of(
                        "id", "12345",
                        "email", "naver@example.com"
                )
        ));

        assertThat(userInfo.nickname()).isEqualTo("naver");
    }

    @Test
    void googleRejectsUnverifiedEmail() {
        GoogleOAuth2Strategy strategy = new GoogleOAuth2Strategy();

        assertThatThrownBy(() -> strategy.extract(Map.of(
                "sub", "12345",
                "email", "google@example.com",
                "email_verified", false,
                "name", "googleUser"
        ))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void googleUsesEmailPrefixWhenNameIsMissing() {
        GoogleOAuth2Strategy strategy = new GoogleOAuth2Strategy();

        OAuth2UserInfo userInfo = strategy.extract(Map.of(
                "sub", "12345",
                "email", "google@example.com",
                "email_verified", true
        ));

        assertThat(userInfo.nickname()).isEqualTo("google");
    }

    @Test
    void kakaoRejectsMissingOrUnverifiedEmail() {
        KakaoOAuth2Strategy strategy = new KakaoOAuth2Strategy();

        assertThatThrownBy(() -> strategy.extract(Map.of(
                "id", 12345,
                "kakao_account", Map.of(
                        "has_email", true,
                        "is_email_valid", true,
                        "is_email_verified", false,
                        "email", "kakao@example.com",
                        "profile", Map.of("nickname", "kakaoUser")
                )
        ))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void kakaoUsesEmailPrefixWhenNicknameIsMissing() {
        KakaoOAuth2Strategy strategy = new KakaoOAuth2Strategy();

        OAuth2UserInfo userInfo = strategy.extract(Map.of(
                "id", 12345,
                "kakao_account", Map.of(
                        "has_email", true,
                        "is_email_valid", true,
                        "is_email_verified", true,
                        "email", "kakao@example.com"
                )
        ));

        assertThat(userInfo.nickname()).isEqualTo("kakao");
    }
    
    
}
