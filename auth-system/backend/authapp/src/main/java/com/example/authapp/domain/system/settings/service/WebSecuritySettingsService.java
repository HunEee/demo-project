package com.example.authapp.domain.system.settings.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.authapp.domain.system.settings.entity.WebSecuritySettingsEntity;
import com.example.authapp.domain.system.settings.repository.WebSecuritySettingsRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WebSecuritySettingsService {

    private final WebSecuritySettingsRepository repository;

    @Transactional(readOnly = true)
    public WebSecuritySettingsEntity current() {
        return repository.findById(WebSecuritySettingsEntity.SETTINGS_ID).orElseGet(WebSecuritySettingsEntity::defaults);
    }
}
