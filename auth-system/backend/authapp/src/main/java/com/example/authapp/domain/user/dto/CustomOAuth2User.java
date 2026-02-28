package com.example.authapp.domain.user.dto;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;

// Spring Security가 로그인된 사용자(principal) 로 인식
public class CustomOAuth2User implements OAuth2User {
	
	// OAuth2 제공자(네이버, 구글 등)에서 받아온 원본 사용자 정보
	private final Map<String, Object> attributes;
	// 사용자 권한 정보 
    private final Collection<? extends GrantedAuthority> authorities;
    // 애플리케이션에서 사용할 고유 사용자 식별자
    private final String username;

    public CustomOAuth2User(Map<String, Object> attributes,
                            Collection<? extends GrantedAuthority> authorities,
                            String username) {
        this.attributes = attributes;
        this.authorities = authorities;
        this.username = username;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getName() {
        return username;
    }

}
