package com.example.authapp.domain.jwt.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.authapp.domain.jwt.entity.TokenSettingsEntity;

public interface TokenSettingsRepository extends JpaRepository<TokenSettingsEntity, Long> {
}
