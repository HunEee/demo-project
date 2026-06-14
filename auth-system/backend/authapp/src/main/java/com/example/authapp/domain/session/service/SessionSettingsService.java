package com.example.authapp.domain.session.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.authapp.domain.session.entity.SessionSettingsEntity;
import com.example.authapp.domain.session.repository.SessionSettingsRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SessionSettingsService {

    private final SessionSettingsRepository repository;

    @Transactional(readOnly = true)
    public SessionSettingsEntity current() {
        return repository.findById(SessionSettingsEntity.SETTINGS_ID).orElseGet(SessionSettingsEntity::defaults);
    }
}
