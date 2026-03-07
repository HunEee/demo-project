package com.example.authapp.domain.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.authapp.domain.user.entity.RoleEntity;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<RoleEntity, Long> {
    Optional<RoleEntity> findByName(String name);
}
