package com.example.authapp.domain.system.settings.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.authapp.domain.system.settings.entity.WebSecuritySettingsEntity;

public interface WebSecuritySettingsRepository extends JpaRepository<WebSecuritySettingsEntity, Long> {
}
