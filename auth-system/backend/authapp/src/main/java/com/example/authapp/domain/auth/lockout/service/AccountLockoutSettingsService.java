package com.example.authapp.domain.auth.lockout.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.authapp.domain.auth.lockout.entity.AccountLockoutSettingsEntity;
import com.example.authapp.domain.auth.lockout.repository.AccountLockoutSettingsRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AccountLockoutSettingsService {

    private final AccountLockoutSettingsRepository repository;

    @Transactional(readOnly = true)
    public AccountLockoutSettingsEntity current() {
        return repository.findById(AccountLockoutSettingsEntity.SETTINGS_ID)
                .orElseGet(AccountLockoutSettingsEntity::defaults);
    }
}
