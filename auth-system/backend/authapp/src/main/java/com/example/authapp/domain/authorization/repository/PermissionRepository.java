package com.example.authapp.domain.authorization.repository;

import java.util.List;
import java.util.Optional;
import java.util.Collection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.authapp.domain.authorization.entity.PermissionEntity;

public interface PermissionRepository extends JpaRepository<PermissionEntity, Long> {

    Optional<PermissionEntity> findByCode(String code);

    List<PermissionEntity> findByCodeIn(Collection<String> codes);

    boolean existsByCode(String code);

    @Query("""
            select distinct p.code
            from UserEntity u
            join u.roles r
            join r.permissions p
            where u.username = :username
              and u.enabled = true
              and u.locked = false
              and u.deletedAt is null
              and r.enabled = true
              and p.enabled = true
            """)
    List<String> findEnabledDirectPermissionCodesByUsername(@Param("username") String username);

    @Query("""
            select distinct p.code
            from GroupUserEntity gu
            join gu.group g
            join g.roles r
            join r.permissions p
            where gu.username = :username
              and g.enabled = true
              and r.enabled = true
              and p.enabled = true
            """)
    List<String> findEnabledGroupPermissionCodesByUsername(@Param("username") String username);
}
