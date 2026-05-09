package com.example.authapp.security.handler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import com.example.authapp.domain.audit.service.LoginHistoryService;
import com.example.authapp.domain.audit.service.AuthEventLogService;
import com.example.authapp.domain.risk.service.RiskService;
import com.example.authapp.domain.user.repository.UserRepository;
import com.example.authapp.util.ClientUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class LoginFailureHandler implements AuthenticationFailureHandler {

	private final UserRepository userRepository;
	private final AuthEventLogService authEventLogService;
	private final LoginHistoryService loginHistoryService;
    private final RiskService riskService;
	
    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception)
            throws IOException, ServletException {
    	 	response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    	    response.setContentType("application/json;charset=UTF-8");

    	    Map<String, Object> result = new HashMap<>();
    	    result.put("message", exception.getMessage());
    	    result.put("error", "LOGIN_FAILED");
    	    result.put("status", 401);

    	    ObjectMapper objectMapper = new ObjectMapper();
    	    response.getWriter().write(objectMapper.writeValueAsString(result));

    	    // username 추출 (필터 or request param)
    	    String username = (String) request.getAttribute("username");
    	    if (username == null || username.isBlank()) {
    	        username = request.getParameter("username");
    	    }

    	    // username 없으면 로그 안 남김
    	    if (username == null || username.isBlank()) {
    	        return;
    	    }

    	    // 유저 존재 여부 체크
    	    var userOpt = userRepository.findByUsername(username);

    	    // 클라이언트 정보
    	    String ip = ClientUtil.getIp(request);
    	    String userAgent = ClientUtil.getUserAgent(request);
    	    String device = ClientUtil.getDevice(userAgent);

    	    String reason = "로그인 실패";

    	    // 1. 유저 존재
    	    if (userOpt.isPresent()) {

    	        var user = userOpt.get();

    	        // 비밀번호 오류
    	        if (exception instanceof BadCredentialsException) {
    	            reason = "비밀번호 오류";
    	        } else {
    	            reason = exception.getMessage();
    	        }
    	        var history = loginHistoryService.saveFail(username, ip, userAgent, device, reason);
    	        // user 전달 
    	        riskService.analyzeLoginRisk(user, history);
    	        authEventLogService.loginFail(username, reason);
    	    }

    	    // 2. 유저 없음
    	    else {
    	        authEventLogService.loginFail(username, "존재하지 않는 사용자");
    	    }
        
    }
    
}
