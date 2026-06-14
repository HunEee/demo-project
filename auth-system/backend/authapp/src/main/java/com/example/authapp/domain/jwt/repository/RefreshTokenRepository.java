package com.example.authapp.domain.jwt.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.example.authapp.domain.jwt.entity.RefreshTokenEntity;

import jakarta.persistence.LockModeType;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, Long> {

    Boolean existsByRefreshTokenHash(String refreshTokenHash);

    Boolean existsByUsernameAndJtiAndRevokedFalse(String username, String jti);

    @Transactional
    void deleteByUsername(String username);

    @Transactional
    void deleteByCreatedAtBefore(LocalDateTime createdAt);

    Optional<RefreshTokenEntity> findByRefreshTokenHash(String refreshTokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT r
            FROM RefreshTokenEntity r
            WHERE r.refreshTokenHash = :refreshTokenHash
            """)
    Optional<RefreshTokenEntity> findByRefreshTokenHashForUpdate(@Param("refreshTokenHash") String refreshTokenHash);

    Optional<RefreshTokenEntity> findByUsernameAndJtiAndRevokedFalse(String username, String jti);

    Optional<RefreshTokenEntity> findByFamilyIdAndJtiAndRevokedFalse(String familyId, String jti);

    List<RefreshTokenEntity> findByUsername(String username);

    List<RefreshTokenEntity> findByUsernameAndRevokedFalse(String username);

    @Query("""
            SELECT r
            FROM RefreshTokenEntity r
            WHERE r.familyId = :familyId
              AND r.revoked = false
            ORDER BY r.tokenSequence DESC
            """)
    List<RefreshTokenEntity> findActiveByFamilyId(@Param("familyId") String familyId);

    @Query("""
            SELECT r
            FROM RefreshTokenEntity r
            LEFT JOIN FETCH r.loginHistory
            WHERE r.username = :username
              AND r.revoked = false
            """)
    List<RefreshTokenEntity> findActiveSessionsByUsername(@Param("username") String username);

    Optional<RefreshTokenEntity> findByIdAndUsername(Long id, String username);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update RefreshTokenEntity token
            set token.revoked = true,
                token.revokedReason = :reason,
                token.revokedBy = :actor,
                token.revokedAt = :revokedAt
            where token.username = :username
              and token.revoked = false
            """)
    int revokeActiveByUsername(
            @Param("username") String username,
            @Param("reason") String reason,
            @Param("actor") String actor,
            @Param("revokedAt") LocalDateTime revokedAt
    );
}
