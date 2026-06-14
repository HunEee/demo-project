package com.example.authapp.application.audit.usecase;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import com.example.authapp.domain.audit.dto.LoginHistoryResponseDTO;
import com.example.authapp.domain.audit.service.LoginHistoryService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LoginHistoryUseCase {

    private final LoginHistoryService loginHistoryService;

    public Page<LoginHistoryResponseDTO> getLoginHistories(String username, int page, int size, String date) {
        return loginHistoryService.getLoginHistories(username, page, size, date);
    }
}
