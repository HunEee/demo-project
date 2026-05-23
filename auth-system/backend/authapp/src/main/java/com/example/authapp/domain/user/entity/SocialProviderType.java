package com.example.authapp.domain.user.entity;

import lombok.Getter;

@Getter
public enum SocialProviderType {

    NAVER("NAVER"),
    GOOGLE("GOOGLE"),
    KAKAO("KAKAO");

    private final String description;

    SocialProviderType(String description) {
        this.description = description;
    }

}
