package com.example.authapp.security.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

class LoginFilterTest {

    @Test
    void requiresAuthenticationForAuthLoginPath() {
        TestableLoginFilter filter = newFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");

        assertThat(filter.matches(request)).isTrue();
    }

    @Test
    void keepsLegacyLoginPathDuringTransition() {
        TestableLoginFilter filter = newFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/login");

        assertThat(filter.matches(request)).isTrue();
    }

    private TestableLoginFilter newFilter() {
        return new TestableLoginFilter(
                mock(AuthenticationManager.class),
                mock(AuthenticationSuccessHandler.class),
                mock(AuthenticationFailureHandler.class),
                "/api/v1"
        );
    }

    private static class TestableLoginFilter extends LoginFilter {

        TestableLoginFilter(
                AuthenticationManager authenticationManager,
                AuthenticationSuccessHandler successHandler,
                AuthenticationFailureHandler failureHandler,
                String apiPrefix
        ) {
            super(authenticationManager, successHandler, failureHandler, apiPrefix);
        }

        boolean matches(MockHttpServletRequest request) {
            return requiresAuthentication(request, new MockHttpServletResponse());
        }
    }
}
