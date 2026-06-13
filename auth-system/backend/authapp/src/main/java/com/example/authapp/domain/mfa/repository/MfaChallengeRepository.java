package com.example.authapp.domain.mfa.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.authapp.domain.mfa.entity.MfaChallengeEntity;

import jakarta.persistence.LockModeType;

public interface MfaChallengeRepository extends JpaRepository<MfaChallengeEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from MfaChallengeEntity c where c.challengeId = :challengeId and c.used = false")
    Optional<MfaChallengeEntity> findByChallengeIdAndUsedFalseForUpdate(@Param("challengeId") String challengeId);

    @Modifying
    @Query("update MfaChallengeEntity c set c.used = true where c.username = :username and c.used = false")
    void expireOpenChallenges(@Param("username") String username);
}
