package com.example.authapp.domain.authorization.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.authapp.domain.authorization.entity.RoleEntity;

public interface RoleRepository extends JpaRepository<RoleEntity, Long> {

    Optional<RoleEntity> findByName(String name);

    List<RoleEntity> findByNameIn(Collection<String> names);

    boolean existsByName(String name);

    @Query("select r from RoleEntity r left join fetch r.permissions where r.name = :name")
    Optional<RoleEntity> findWithPermissionsByName(@Param("name") String name);
}
