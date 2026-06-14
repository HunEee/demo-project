package com.example.authapp.domain.system.settings.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.authapp.domain.system.settings.entity.RateLimitSettingsEntity;

public interface RateLimitSettingsRepository extends JpaRepository<RateLimitSettingsEntity, Long> {
}
