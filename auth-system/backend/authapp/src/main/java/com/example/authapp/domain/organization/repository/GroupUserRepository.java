package com.example.authapp.domain.organization.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.authapp.domain.organization.entity.GroupUserEntity;

public interface GroupUserRepository extends JpaRepository<GroupUserEntity, Long> {

    List<GroupUserEntity> findByUsername(String username);

    List<GroupUserEntity> findByUsernameIn(Collection<String> usernames);

    List<GroupUserEntity> findByGroupId(Long groupId);

    long countByGroupId(Long groupId);

    boolean existsByGroupIdAndUsername(Long groupId, String username);

    Optional<GroupUserEntity> findByGroupIdAndUsername(Long groupId, String username);
    
}
