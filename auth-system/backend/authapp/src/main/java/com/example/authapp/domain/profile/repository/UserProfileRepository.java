package com.example.authapp.domain.profile.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.authapp.domain.profile.entity.UserProfileEntity;

public interface UserProfileRepository extends JpaRepository<UserProfileEntity, Long> {

    Optional<UserProfileEntity> findByUsername(String username);

    List<UserProfileEntity> findByUsernameIn(List<String> usernames);

    List<UserProfileEntity> findByDepartmentId(Long departmentId);

    long countByDepartmentId(Long departmentId);
    
}
