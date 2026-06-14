package com.example.authapp.domain.session.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.authapp.domain.session.entity.SessionSettingsEntity;

public interface SessionSettingsRepository extends JpaRepository<SessionSettingsEntity, Long> {
}
