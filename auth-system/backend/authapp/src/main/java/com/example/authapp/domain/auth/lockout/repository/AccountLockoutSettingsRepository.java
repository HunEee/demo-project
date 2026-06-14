package com.example.authapp.domain.auth.lockout.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.authapp.domain.auth.lockout.entity.AccountLockoutSettingsEntity;

public interface AccountLockoutSettingsRepository extends JpaRepository<AccountLockoutSettingsEntity, Long> {
}
