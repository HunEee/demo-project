package com.example.authapp.domain.mfa.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.authapp.domain.mfa.entity.MfaChallengeEntity;

public interface MfaChallengeRepository extends JpaRepository<MfaChallengeEntity, Long> {

    Optional<MfaChallengeEntity> findByChallengeIdAndUsedFalse(String challengeId);

    @Modifying
    @Query("update MfaChallengeEntity c set c.used = true where c.username = :username and c.used = false")
    void expireOpenChallenges(@Param("username") String username);
}
