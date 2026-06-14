package com.example.authapp.application.admin.usecase;

import org.springframework.stereotype.Service;

import com.example.authapp.domain.admin.AdminSettingsStore;
import com.example.authapp.domain.admin.dto.AdminSettingsResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminSettingsUseCase {

    private final AdminSettingsStore store;

    public AdminSettingsResponse current() {
        return store.current();
    }

    public AdminSettingsResponse update(AdminSettingsResponse request) {
        return store.update(request);
    }
}
