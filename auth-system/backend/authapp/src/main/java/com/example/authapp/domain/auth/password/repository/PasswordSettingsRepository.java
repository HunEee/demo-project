package com.example.authapp.domain.auth.password.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.authapp.domain.auth.password.entity.PasswordSettingsEntity;

public interface PasswordSettingsRepository extends JpaRepository<PasswordSettingsEntity, Long> {
}
