package com.example.authapp.domain.admin.security.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.authapp.domain.admin.security.entity.AdminSecuritySettingsEntity;

public interface AdminSecuritySettingsRepository extends JpaRepository<AdminSecuritySettingsEntity, Long> {
}
