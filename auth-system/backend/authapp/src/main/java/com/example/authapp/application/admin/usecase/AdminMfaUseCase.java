package com.example.authapp.application.admin.usecase;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.authapp.domain.mfa.dto.AdminMfaUserResponse;
import com.example.authapp.domain.mfa.dto.MfaExceptionRequest;
import com.example.authapp.domain.mfa.dto.MfaPolicyResponse;
import com.example.authapp.domain.mfa.entity.MfaPolicy;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminMfaUseCase {

    private final AdminMfaService service;

    public List<AdminMfaUserResponse> users() {
        return service.users();
    }

    public AdminMfaUserResponse user(String username) {
        return service.user(username);
    }

    public void reset(String username, String reason, HttpServletRequest request) {
        service.reset(username, reason, request);
    }

    public void createException(String username, MfaExceptionRequest request, HttpServletRequest httpRequest) {
        service.createException(username, request, httpRequest);
    }

    public void revokeException(String username, HttpServletRequest request) {
        service.revokeException(username, request);
    }

    public MfaPolicyResponse policy() {
        return service.policy();
    }

    public MfaPolicyResponse updatePolicy(MfaPolicy policy, HttpServletRequest request) {
        return service.updatePolicy(policy, request);
    }
}
