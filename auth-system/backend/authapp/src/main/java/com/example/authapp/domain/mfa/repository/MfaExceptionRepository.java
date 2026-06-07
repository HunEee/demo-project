package com.example.authapp.domain.mfa.repository;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.authapp.domain.mfa.entity.MfaExceptionEntity;

public interface MfaExceptionRepository extends JpaRepository<MfaExceptionEntity, Long> {

    default Optional<MfaExceptionEntity> findActiveByUsername(String username) {
        return findByUsernameAndRevokedAtIsNullAndExpiresAtAfterOrderByExpiresAtDesc(username, LocalDateTime.now())
                .stream()
                .findFirst();
    }

    List<MfaExceptionEntity> findByUsernameAndRevokedAtIsNullAndExpiresAtAfterOrderByExpiresAtDesc(String username, LocalDateTime now);

    @Query("""
            select e from MfaExceptionEntity e
            where e.username = :username
              and e.revokedAt is null
              and e.expiresAt > current_timestamp
            order by e.expiresAt desc
            """)
    List<MfaExceptionEntity> findActiveCandidatesByUsername(@Param("username") String username);

    @Query("""
            select e from MfaExceptionEntity e
            where e.username in :usernames
              and e.revokedAt is null
              and e.expiresAt > current_timestamp
            order by e.expiresAt desc
            """)
    List<MfaExceptionEntity> findActiveByUsernameIn(@Param("usernames") List<String> usernames);

    List<MfaExceptionEntity> findByUsernameOrderByCreatedAtDesc(String username);
}
