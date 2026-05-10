package com.example.authapp.security.handler;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.example.authapp.application.auth.AuthFacade;
import com.example.authapp.application.auth.dto.LoginResponseDTO;
import com.example.authapp.security.principal.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component("loginSuccessHandler")
@RequiredArgsConstructor
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final AuthFacade authFacade;
    
	@Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

    	// 실제 유저 꺼내기
    	UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        LoginResponseDTO result = authFacade.loginSuccess(principal, request, response);
        
        // 응답
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        new ObjectMapper().writeValue(response.getWriter(), result);
        
    }
    


}