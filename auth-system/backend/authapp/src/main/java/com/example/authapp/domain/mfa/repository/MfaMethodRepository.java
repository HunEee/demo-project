package com.example.authapp.domain.mfa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.authapp.domain.mfa.entity.MfaMethodEntity;

public interface MfaMethodRepository extends JpaRepository<MfaMethodEntity, Long> {

    boolean existsByUsernameAndEnabledTrue(String username);

    List<MfaMethodEntity> findByUsername(String username);

    void deleteByUsername(String username);
}
