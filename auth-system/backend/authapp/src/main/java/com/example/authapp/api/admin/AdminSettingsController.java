package com.example.authapp.api.admin;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.authapp.application.admin.usecase.AdminSettingsUseCase;
import com.example.authapp.domain.admin.dto.AdminSettingsResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/settings")
@RequiredArgsConstructor
public class AdminSettingsController {

    private final AdminSettingsUseCase adminSettingsStore;

    // 현재 관리자 보안 설정 값을 조회한다.
    @GetMapping
    public AdminSettingsResponse settings() {
        return adminSettingsStore.current();
    }

    // 관리자 보안 설정 값을 갱신한다.
    @PatchMapping
    public AdminSettingsResponse update(@RequestBody AdminSettingsResponse request) {
        return adminSettingsStore.update(request);
    }

    
}
