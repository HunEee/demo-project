package com.example.authapp.domain.admin;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.authapp.domain.admin.dto.AdminSettingsResponse;
import com.example.authapp.domain.mfa.entity.MfaPolicy;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@Transactional
public class AdminSettingsStore {

    private static final Long SETTINGS_ID = 1L;

    private final AdminSettingsRepository repository;

    public AdminSettingsResponse current() {
        return currentEntity().toResponse();
    }

    public AdminSettingsResponse update(AdminSettingsResponse request) {
        AdminSettingsEntity settings = currentEntity();
        settings.update(request);
        return repository.save(settings).toResponse();
    }

    public MfaPolicy mfaPolicy() {
        MfaPolicy policy = currentEntity().getMfaPolicy();
        return policy == null ? MfaPolicy.OPTIONAL : policy.normalized();
    }

    public MfaPolicy updateMfaPolicy(MfaPolicy mfaPolicy) {
        AdminSettingsEntity settings = currentEntity();
        settings.updateMfaPolicy(mfaPolicy);
        return repository.save(settings).getMfaPolicy();
    }

    private AdminSettingsEntity currentEntity() {
        return repository.findById(SETTINGS_ID)
                .orElseGet(() -> repository.save(AdminSettingsEntity.defaults()));
    }
}
