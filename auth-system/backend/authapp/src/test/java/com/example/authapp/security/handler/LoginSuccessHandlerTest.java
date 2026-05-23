package com.example.authapp.security.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;

import com.example.authapp.application.auth.AuthFacade;
import com.example.authapp.application.auth.dto.LoginResponseDTO;
import com.example.authapp.security.handler.dto.UserResponseDTO;
import com.example.authapp.security.principal.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;

class LoginSuccessHandlerTest {

    @Test
    void writesLoginResponseWithLocalDateTimeFields() throws Exception {
        AuthFacade authFacade = mock(AuthFacade.class);
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        LoginSuccessHandler handler = new LoginSuccessHandler(authFacade, objectMapper);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        UserPrincipal principal = mock(UserPrincipal.class);
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(principal, null);
        LoginResponseDTO loginResponse = LoginResponseDTO.builder()
                .accessToken("access-token")
                .expiresIn(36L)
                .user(UserResponseDTO.builder()
                        .id(1L)
                        .username("hong1")
                        .email("hong1@example.com")
                        .nickname("hong1")
                        .enabled(true)
                        .provider("LOCAL")
                        .roles(Set.of("ROLE_USER"))
                        .createdAt(LocalDateTime.of(2026, 5, 23, 13, 23, 41))
                        .updatedAt(LocalDateTime.of(2026, 5, 23, 13, 23, 41))
                        .build())
                .build();

        when(authFacade.loginSuccess(eq(principal), eq(request), eq(response))).thenReturn(loginResponse);

        handler.onAuthenticationSuccess(request, response, authentication);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentAsString()).contains("\"accessToken\":\"access-token\"");
        assertThat(response.getContentAsString()).contains("\"createdAt\"");
    }
    
    
}
