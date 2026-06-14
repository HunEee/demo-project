package com.example.authapp.security.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

import com.example.authapp.domain.audit.entity.LoginHistoryEntity;
import com.example.authapp.domain.audit.service.AuthEventLogService;
import com.example.authapp.domain.audit.service.LoginHistoryService;
import com.example.authapp.application.risk.usecase.RiskLoginDetectionService;
import com.example.authapp.application.risk.usecase.RiskService;
import com.example.authapp.domain.user.entity.UserEntity;
import com.example.authapp.domain.user.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

class LoginFailureHandlerTest {

    @Test
    void returnsSameUnauthorizedMessageForExistingAndMissingUsers() throws Exception {
        HandlerFixture fixture = new HandlerFixture();
        when(fixture.userRepository.findByUsernameWithRoles("alice")).thenReturn(Optional.of(UserEntity.builder().username("alice").enabled(true).build()));
        when(fixture.userRepository.findByUsernameWithRoles("missing")).thenReturn(Optional.empty());
        when(fixture.loginHistoryService.saveFail(eq("alice"), any(), any(), any(), any()))
                .thenReturn(LoginHistoryEntity.builder().username("alice").success(false).build());

        MockHttpServletResponse existingResponse = fixture.fail("alice");
        MockHttpServletResponse missingResponse = fixture.fail("missing");

        assertThat(existingResponse.getStatus()).isEqualTo(401);
        assertThat(missingResponse.getStatus()).isEqualTo(401);
        assertThat(readBody(existingResponse)).isEqualTo(readBody(missingResponse));
        assertThat(readBody(existingResponse))
                .containsEntry("error", "LOGIN_FAILED")
                .containsEntry("status", 401);
        assertThat(existingResponse.getContentAsString()).doesNotContain("raw failure");
        assertThat(missingResponse.getContentAsString()).doesNotContain("raw failure");
    }

    @Test
    void stillReturnsUnauthorizedWhenRiskLoggingFails() throws Exception {
        HandlerFixture fixture = new HandlerFixture();
        UserEntity user = UserEntity.builder().username("alice").enabled(true).build();
        LoginHistoryEntity history = LoginHistoryEntity.builder().username("alice").success(false).build();
        when(fixture.userRepository.findByUsernameWithRoles("alice")).thenReturn(Optional.of(user));
        when(fixture.loginHistoryService.saveFail(eq("alice"), any(), any(), any(), any())).thenReturn(history);
        when(fixture.riskLoginDetectionService.detectFailure(user, history)).thenThrow(new IllegalStateException("risk failed"));

        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatNoException().isThrownBy(() -> fixture.handler.onAuthenticationFailure(
                fixture.request("alice"),
                response,
                new BadCredentialsException("raw failure")
        ));
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("LOGIN_FAILED");
    }

    @Test
    void invokesRiskDetectionForExistingUserFailure() throws Exception {
        HandlerFixture fixture = new HandlerFixture();
        UserEntity user = UserEntity.builder().username("alice").enabled(true).build();
        LoginHistoryEntity history = LoginHistoryEntity.builder().username("alice").success(false).build();
        when(fixture.userRepository.findByUsernameWithRoles("alice")).thenReturn(Optional.of(user));
        when(fixture.loginHistoryService.saveFail(eq("alice"), any(), any(), any(), any())).thenReturn(history);
        when(fixture.riskLoginDetectionService.detectFailure(user, history)).thenReturn(60);

        fixture.fail("alice");

        verify(fixture.riskLoginDetectionService).detectFailure(user, history);
        verify(fixture.riskService).applyLoginRuleScore(user, history, 60, "RULE_BASED_LOGIN_FAILURE");
        verify(fixture.riskService, never()).analyzeLoginRisk(user, history);
    }

    private static final class HandlerFixture {
        private final UserRepository userRepository = mock(UserRepository.class);
        private final AuthEventLogService authEventLogService = mock(AuthEventLogService.class);
        private final LoginHistoryService loginHistoryService = mock(LoginHistoryService.class);
        private final RiskService riskService = mock(RiskService.class);
        private final RiskLoginDetectionService riskLoginDetectionService = mock(RiskLoginDetectionService.class);
        private final LoginFailureHandler handler = new LoginFailureHandler(
                userRepository,
                authEventLogService,
                loginHistoryService,
                riskService,
                riskLoginDetectionService
        );

        private MockHttpServletRequest request(String username) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
            request.setAttribute("username", username);
            request.addHeader("User-Agent", "JUnit");
            request.setRemoteAddr("127.0.0.1");
            return request;
        }

        private MockHttpServletResponse fail(String username) throws Exception {
            MockHttpServletResponse response = new MockHttpServletResponse();
            handler.onAuthenticationFailure(request(username), response, new BadCredentialsException("raw failure"));
            return response;
        }
    }

    private Map<String, Object> readBody(MockHttpServletResponse response) throws Exception {
        return new ObjectMapper().readValue(response.getContentAsString(), new TypeReference<>() {});
    }
}
