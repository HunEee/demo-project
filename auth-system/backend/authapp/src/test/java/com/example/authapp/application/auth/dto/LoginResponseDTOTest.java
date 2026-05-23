package com.example.authapp.application.auth.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

class LoginResponseDTOTest {

    @Test
    void doesNotExposeRefreshTokenInResponseBody() {
        boolean hasRefreshTokenField = Arrays.stream(LoginResponseDTO.class.getDeclaredFields())
                .anyMatch(field -> field.getName().equals("refreshToken"));

        assertThat(hasRefreshTokenField).isFalse();
    }
    
    
}
