package com.example.authapp.security.handler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import com.example.authapp.domain.audit.service.AuthEventLogService;
import com.example.authapp.domain.audit.service.LoginHistoryService;
import com.example.authapp.application.risk.usecase.RiskLoginDetectionService;
import com.example.authapp.application.risk.usecase.RiskService;
import com.example.authapp.domain.user.repository.UserRepository;
import com.example.authapp.util.ClientUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoginFailureHandler implements AuthenticationFailureHandler {

    private static final String LOGIN_FAILED_MESSAGE = "아이디 또는 비밀번호가 올바르지 않습니다.";

    private final UserRepository userRepository;
    private final AuthEventLogService authEventLogService;
    private final LoginHistoryService loginHistoryService;
    private final RiskService riskService;
    private final RiskLoginDetectionService riskLoginDetectionService;

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {
        recordFailureSafely(request, exception);
        writeFailureResponse(response);
    }

    private void recordFailureSafely(HttpServletRequest request, AuthenticationException exception) {
        try {
            recordFailure(request, exception);
        } catch (Exception loggingException) {
            log.warn("Failed to record login failure audit or risk event.", loggingException);
        }
    }

    private void recordFailure(HttpServletRequest request, AuthenticationException exception) {
        String username = extractUsername(request);
        if (username == null || username.isBlank()) {
            return;
        }

        // 로그인 실패 기록에 필요한 클라이언트 정보를 수집한다.
        String ip = ClientUtil.getIp(request);
        String userAgent = ClientUtil.getUserAgent(request);
        String device = ClientUtil.getDevice(userAgent);
        String reason = exception instanceof BadCredentialsException ? "BAD_CREDENTIALS" : "LOGIN_FAILED";

        // 사용자 존재 여부에 따라 감사 로그와 위험도 분석 범위를 결정한다.
        var userOpt = userRepository.findByUsernameWithRoles(username);
        if (userOpt.isEmpty()) {
            authEventLogService.loginFail(username, reason);
            return;
        }

        // 로그인 실패 이력과 룰 기반 위험 점수를 기록한다.
        var user = userOpt.get();
        var history = loginHistoryService.saveFail(username, ip, userAgent, device, reason);
        int ruleScore = riskLoginDetectionService.detectFailure(user, history);
        if (ruleScore > 0) {
            riskService.applyLoginRuleScore(user, history, ruleScore, "RULE_BASED_LOGIN_FAILURE");
        }
        authEventLogService.loginFail(username, reason);
    }

    private String extractUsername(HttpServletRequest request) {
        Object attribute = request.getAttribute("username");
        if (attribute instanceof String username && !username.isBlank()) {
            return username;
        }
        return request.getParameter("username");
    }

    private void writeFailureResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");

        Map<String, Object> result = new HashMap<>();
        result.put("message", LOGIN_FAILED_MESSAGE);
        result.put("error", "LOGIN_FAILED");
        result.put("status", 401);

        ObjectMapper objectMapper = new ObjectMapper();
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
