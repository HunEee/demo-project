package com.example.authapp.security.handler;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.example.authapp.application.auth.AuthFacade;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component("socialSuccessHandler")
@RequiredArgsConstructor
public class LoginSuccessSocialHandler implements AuthenticationSuccessHandler {
	
    private final AuthFacade authFacade;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
    	authFacade.socialLoginSuccess(request, response, authentication);
    }

    
}
