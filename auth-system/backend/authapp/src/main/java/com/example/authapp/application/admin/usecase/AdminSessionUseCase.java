package com.example.authapp.application.admin.usecase;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import com.example.authapp.domain.admin.dto.AdminSessionResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminSessionUseCase {

    private final AdminConsoleUseCase adminConsoleUseCase;

    public Page<AdminSessionResponse> sessions(int page, int size, String username, Boolean activeOnly, String status, String device, String from, String to, String sort, String direction) {
        return adminConsoleUseCase.sessions(page, size, username, activeOnly, status, device, from, to, sort, direction);
    }

    public List<AdminSessionResponse> sessionsByUsername(String username) {
        return adminConsoleUseCase.sessionsByUsername(username);
    }

    public void revokeSession(Long id) {
        adminConsoleUseCase.revokeSession(id);
    }

    public void revokeUserSessions(String username) {
        adminConsoleUseCase.revokeUserSessions(username);
    }

    public void revokeUserTokens(String username) {
        adminConsoleUseCase.revokeUserTokens(username);
    }
}
