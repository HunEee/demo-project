package com.example.authapp.domain.user.oauth;

//공급자별 사용자 정보 표준화 DTO
public record OAuth2UserInfo(
        String provider,
        String providerId,
        String email,
        String nickname
) {
    public String username() {
        return provider + "_" + providerId;
    }
}