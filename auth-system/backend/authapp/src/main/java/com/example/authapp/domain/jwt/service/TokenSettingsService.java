package com.example.authapp.domain.jwt.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.authapp.domain.jwt.entity.TokenSettingsEntity;
import com.example.authapp.domain.jwt.repository.TokenSettingsRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TokenSettingsService {

    private final TokenSettingsRepository repository;

    @Transactional(readOnly = true)
    public TokenSettingsEntity current() {
        return repository.findById(TokenSettingsEntity.SETTINGS_ID).orElseGet(TokenSettingsEntity::defaults);
    }
}
