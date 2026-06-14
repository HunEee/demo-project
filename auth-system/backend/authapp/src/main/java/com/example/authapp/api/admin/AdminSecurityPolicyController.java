package com.example.authapp.api.admin;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.authapp.domain.admin.settings.dto.SecuritySettingsCenterResponse;
import com.example.authapp.domain.admin.settings.dto.SecuritySettingsUpdateRequest;
import com.example.authapp.application.admin.usecase.AdminSecurityPolicyUseCase;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping({"/api/v1/admin/security-policies", "/api/v1/admin/security-settings"})
@RequiredArgsConstructor
public class AdminSecurityPolicyController {

    private final AdminSecurityPolicyUseCase securitySettingsCenterService;

    @GetMapping
    public SecuritySettingsCenterResponse policies() {
        return securitySettingsCenterService.currentSettings();
    }

    @PatchMapping
    public SecuritySettingsCenterResponse update(
            @RequestBody SecuritySettingsUpdateRequest request,
            @AuthenticationPrincipal(expression = "username") String actorUsername,
            HttpServletRequest httpRequest
    ) {
        return securitySettingsCenterService.update(request, actorUsername, httpRequest);
    }
}
