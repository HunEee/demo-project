package com.example.authapp.application.admin.usecase;

import org.springframework.stereotype.Service;

import com.example.authapp.domain.admin.dto.AdminFilterOptionsResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminFilterOptionsUseCase {

    private final AdminConsoleUseCase adminConsoleUseCase;

    public AdminFilterOptionsResponse filterOptions() {
        return adminConsoleUseCase.filterOptions();
    }
}
