package com.example.authapp.application.admin.usecase;

import org.springframework.stereotype.Service;

import com.example.authapp.domain.admin.settings.dto.SecuritySettingsCenterResponse;
import com.example.authapp.domain.admin.settings.dto.SecuritySettingsUpdateRequest;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminSecurityPolicyUseCase {

    private final SecuritySettingsCenterService service;

    public SecuritySettingsCenterResponse currentSettings() {
        return service.currentSettings();
    }

    public SecuritySettingsCenterResponse update(SecuritySettingsUpdateRequest request, String actorUsername, HttpServletRequest httpRequest) {
        return service.update(request, actorUsername, httpRequest);
    }
}
