package com.example.authapp.domain.authorization.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.authapp.domain.authorization.entity.RoleAssignmentHistoryEntity;

public interface RoleAssignmentHistoryRepository extends JpaRepository<RoleAssignmentHistoryEntity, Long> {

    List<RoleAssignmentHistoryEntity> findByTargetTypeAndTargetIdOrderByCreatedAtDesc(String targetType, String targetId);
}
