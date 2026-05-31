package com.example.authapp.domain.organization.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.authapp.domain.organization.entity.GroupUserEntity;

public interface GroupUserRepository extends JpaRepository<GroupUserEntity, Long> {

    List<GroupUserEntity> findByUsername(String username);
    
}
