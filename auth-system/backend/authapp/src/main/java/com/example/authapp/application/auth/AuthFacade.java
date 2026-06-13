package com.example.authapp.application.auth;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.authapp.application.auth.dto.LoginResponseDTO;
import com.example.authapp.domain.audit.service.AuthEventLogService;
import com.example.authapp.domain.audit.service.LoginHistoryService;
import com.example.authapp.domain.jwt.service.CookieService;
import com.example.authapp.domain.jwt.service.RefreshTokenService;
import com.example.authapp.domain.mfa.dto.MfaChallengeResult;
import com.example.authapp.domain.mfa.dto.MfaVerifyRequest;
import com.example.authapp.domain.mfa.dto.PreAuthTotpConfirmRequest;
import com.example.authapp.domain.mfa.exception.MfaException;
import com.example.authapp.domain.mfa.service.MfaService;
import com.example.authapp.domain.risk.service.RiskService;
import com.example.authapp.domain.user.entity.UserEntity;
import com.example.authapp.domain.user.service.UserQueryService;
import com.example.authapp.security.rbac.RbacAuthorizationService;
import com.example.authapp.security.handler.dto.UserResponseDTO;
import com.example.authapp.security.principal.UserPrincipal;
import com.example.authapp.util.ClientUtil;
import com.example.authapp.util.JWTUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Service
@Transactional
public class AuthFacade {

    private final UserQueryService userQueryService;
    private final RefreshTokenService refreshTokenService;
    private final CookieService cookieService;
    private final LoginHistoryService loginHistoryService;
    private final RiskService riskService;
    private final AuthEventLogService authEventLogService;
    private final MfaService mfaService;
    private final RbacAuthorizationService rbacAuthorizationService;
    private final String frontendUrl;

    public AuthFacade(
            UserQueryService userQueryService,
            RefreshTokenService refreshTokenService,
            CookieService cookieService,
            LoginHistoryService loginHistoryService,
            RiskService riskService,
            AuthEventLogService authEventLogService,
            MfaService mfaService,
            RbacAuthorizationService rbacAuthorizationService,
            @Value("${app.frontend-url:http://localhost:5173}") String frontendUrl
    ) {
        this.userQueryService = userQueryService;
        this.refreshTokenService = refreshTokenService;
        this.cookieService = cookieService;
        this.loginHistoryService = loginHistoryService;
        this.riskService = riskService;
        this.authEventLogService = authEventLogService;
        this.mfaService = mfaService;
        this.rbacAuthorizationService = rbacAuthorizationService;
        this.frontendUrl = frontendUrl;
    }

    public LoginResponseDTO loginSuccess(UserPrincipal principal, HttpServletRequest request, HttpServletResponse response) {
        UserEntity user = userQueryService.getUser(principal.getUserId());
        String username = principal.getUsername();
        Set<String> roles = principal.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        var decision = mfaService.evaluateLogin(username, roles);
        if (decision.required()) {
            MfaChallengeResult challenge = mfaService.createChallenge(
                    username,
                    decision,
                    ClientUtil.getIp(request),
                    ClientUtil.getUserAgent(request)
            );
            return LoginResponseDTO.builder()
                    .mfaRequired(true)
                    .mfaRegistrationRequired(challenge.registrationRequired())
                    .challengeId(challenge.challengeId())
                    .mfaExpiresAt(challenge.expiresAt())
                    .availableMethods(challenge.availableMethods())
                    .build();
        }

        return issueTokens(user, username, roles, request, response);
    }

    @Transactional(noRollbackFor = MfaException.class)
    public LoginResponseDTO completeMfaLogin(MfaVerifyRequest mfaRequest, HttpServletRequest request, HttpServletResponse response) {
        String username = mfaService.verifyChallenge(mfaRequest, ClientUtil.getIp(request), ClientUtil.getUserAgent(request));
        UserEntity user = userQueryService.getByUsername(username);
        Set<String> roles = user.getRoles()
                .stream()
                .map(role -> role.getName())
                .collect(Collectors.toSet());
        return issueTokens(user, username, roles, request, response);
    }

    @Transactional(noRollbackFor = MfaException.class)
    public LoginResponseDTO completeMfaRegistration(PreAuthTotpConfirmRequest mfaRequest, HttpServletRequest request, HttpServletResponse response) {
        String username = mfaService.confirmPreAuthTotpRegistration(mfaRequest, ClientUtil.getIp(request), ClientUtil.getUserAgent(request));
        UserEntity user = userQueryService.getByUsername(username);
        Set<String> roles = user.getRoles()
                .stream()
                .map(role -> role.getName())
                .collect(Collectors.toSet());
        return issueTokens(user, username, roles, request, response);
    }

    public void socialLoginSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        String username = authentication.getName();
        Set<String> roles = authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        var decision = mfaService.evaluateLogin(username, roles);
        if (decision.required()) {
            MfaChallengeResult challenge = mfaService.createChallenge(
                    username,
                    decision,
                    ClientUtil.getIp(request),
                    ClientUtil.getUserAgent(request)
            );
            response.sendRedirect(frontendUrl + "/login/mfa?challengeId=" + challenge.challengeId());
            return;
        }

        String jti = UUID.randomUUID().toString();
        String refreshToken = JWTUtil.createJWT(username, roles, jti, false);
        String ip = ClientUtil.getIp(request);
        String userAgent = ClientUtil.getUserAgent(request);
        String device = ClientUtil.getDevice(userAgent);
        var history = loginHistoryService.saveSuccess(username, ip, userAgent, device);
        UserEntity user = userQueryService.getByUsername(username);

        riskService.analyzeLoginRisk(user, history);
        refreshTokenService.addRefresh(username, refreshToken, ip, userAgent, device, history);
        authEventLogService.loginSuccess(username);
        cookieService.addRefreshCookie(response, refreshToken);
        response.sendRedirect(frontendUrl + "/cookie");
    }

    private LoginResponseDTO issueTokens(
            UserEntity user,
            String username,
            Set<String> roles,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String jti = UUID.randomUUID().toString();
        String accessToken = JWTUtil.createJWT(username, roles, jti, true);
        String refreshToken = JWTUtil.createJWT(username, roles, jti, false);
        long expiresIn = JWTUtil.getAccessTokenExpiresIn();

        String ip = ClientUtil.getIp(request);
        String userAgent = ClientUtil.getUserAgent(request);
        String device = ClientUtil.getDevice(userAgent);
        var history = loginHistoryService.saveSuccess(username, ip, userAgent, device);

        riskService.analyzeLoginRisk(user, history);
        refreshTokenService.addRefresh(username, refreshToken, ip, userAgent, device, history);
        cookieService.addRefreshCookie(response, refreshToken);
        authEventLogService.loginSuccess(username);
        Set<String> permissions = rbacAuthorizationService.findEffectivePermissions(username);

        return LoginResponseDTO.builder()
                .mfaRequired(false)
                .accessToken(accessToken)
                .expiresIn(expiresIn)
                .user(UserResponseDTO.from(user, roles, permissions))
                .build();
    }
}
