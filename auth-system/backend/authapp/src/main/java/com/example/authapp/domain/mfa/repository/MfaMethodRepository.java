package com.example.authapp.domain.mfa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.authapp.domain.mfa.entity.MfaMethodEntity;
import com.example.authapp.domain.mfa.entity.MfaMethodType;

public interface MfaMethodRepository extends JpaRepository<MfaMethodEntity, Long> {

    boolean existsByUsernameAndEnabledTrue(String username);

    List<MfaMethodEntity> findByUsernameAndEnabledTrue(String username);

    Optional<MfaMethodEntity> findByUsernameAndTypeAndEnabledTrue(String username, MfaMethodType type);

    List<MfaMethodEntity> findByUsernameInAndEnabledTrue(List<String> usernames);

    List<MfaMethodEntity> findByUsername(String username);

    void deleteByUsernameAndTypeAndEnabledFalse(String username, MfaMethodType type);

    @Modifying
    @Query("delete from MfaMethodEntity m where m.username = :username and m.type = :type and m.enabled = true")
    void deleteActiveByUsernameAndType(@Param("username") String username, @Param("type") MfaMethodType type);

    void deleteByUsername(String username);
}
