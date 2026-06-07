package com.example.authapp.application.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.example.authapp.domain.audit.entity.LoginHistoryEntity;
import com.example.authapp.domain.audit.service.AuthEventLogService;
import com.example.authapp.domain.audit.service.LoginHistoryService;
import com.example.authapp.domain.jwt.service.CookieService;
import com.example.authapp.domain.jwt.service.RefreshTokenService;
import com.example.authapp.domain.mfa.dto.MfaLoginDecision;
import com.example.authapp.domain.mfa.service.MfaService;
import com.example.authapp.domain.risk.service.RiskService;
import com.example.authapp.domain.user.entity.UserEntity;
import com.example.authapp.domain.user.service.UserQueryService;

@ExtendWith(MockitoExtension.class)
class AuthFacadeTest {

    @Mock
    private UserQueryService userQueryService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private CookieService cookieService;

    @Mock
    private LoginHistoryService loginHistoryService;

    @Mock
    private RiskService riskService;

    @Mock
    private AuthEventLogService authEventLogService;

    @Mock
    private MfaService mfaService;

    @Test
    void socialLoginAnalyzesRiskAndRedirectsToConfiguredFrontendCookiePage() throws Exception {
        AuthFacade authFacade = new AuthFacade(
                userQueryService,
                refreshTokenService,
                cookieService,
                loginHistoryService,
                riskService,
                authEventLogService,
                mfaService,
                "http://frontend.example"
        );
        UserEntity user = UserEntity.builder()
                .username("KAKAO_12345")
                .email("kakao@example.com")
                .nickname("kakao")
                .enabled(true)
                .locked(false)
                .social(true)
                .build();
        LoginHistoryEntity history = LoginHistoryEntity.builder()
                .username("KAKAO_12345")
                .success(true)
                .build();
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(
                "KAKAO_12345",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("User-Agent", "JUnit");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(userQueryService.getByUsername("KAKAO_12345")).thenReturn(user);
        when(mfaService.evaluateLogin(eq("KAKAO_12345"), any())).thenReturn(MfaLoginDecision.notRequired());
        when(loginHistoryService.saveSuccess(eq("KAKAO_12345"), any(), any(), any())).thenReturn(history);

        authFacade.socialLoginSuccess(request, response, authentication);

        verify(riskService).analyzeLoginRisk(user, history);
        assertThat(response.getRedirectedUrl()).isEqualTo("http://frontend.example/cookie");
    }
    
    
}
