package com.example.authapp.api.admin;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.authapp.domain.admin.AdminSettingsStore;
import com.example.authapp.domain.admin.dto.AdminSettingsResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/settings")
@RequiredArgsConstructor
public class AdminSettingsController {

    private final AdminSettingsStore adminSettingsStore;

    // 현재 런타임 보안 정책 값을 조회
    @GetMapping
    public AdminSettingsResponse settings() {
        return adminSettingsStore.current();
    }

    // 런타임 보안 정책 값을 갱신
    @PatchMapping
    public AdminSettingsResponse update(@RequestBody AdminSettingsResponse request) {
        return adminSettingsStore.update(request);
    }

    
}
