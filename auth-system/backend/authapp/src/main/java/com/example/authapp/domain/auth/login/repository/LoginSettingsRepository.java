package com.example.authapp.domain.auth.login.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.authapp.domain.auth.login.entity.LoginSettingsEntity;

public interface LoginSettingsRepository extends JpaRepository<LoginSettingsEntity, Long> {
}
