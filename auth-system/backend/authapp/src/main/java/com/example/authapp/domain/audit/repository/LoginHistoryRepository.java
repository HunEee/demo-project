package com.example.authapp.domain.audit.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.authapp.domain.audit.entity.LoginHistoryEntity;
import com.example.authapp.domain.audit.entity.LoginStatus;
import com.example.authapp.domain.jwt.entity.RefreshTokenEntity;

public interface LoginHistoryRepository extends JpaRepository<LoginHistoryEntity, Long> {
	
    // 사용자 로그인 이력 최신순
    List<LoginHistoryEntity> findByUsernameOrderByLoginAtDesc(String username);

    // 활성 로그인 세션 이력
    List<LoginHistoryEntity> findByUsernameAndStatus(String username,LoginStatus status);

    // 로그인 실패 이력
    List<LoginHistoryEntity> findByUsernameAndSuccessFalseOrderByLoginAtDesc(String username);

    // 최근 로그인 성공 1건
    LoginHistoryEntity findTopByUsernameAndSuccessTrueOrderByLoginAtDesc(String username);
	
    //********************************************************************************
	// 로그인 기록 조회
    //********************************************************************************
	
    // 날짜 필터 없는 경우
	Page<LoginHistoryEntity> findByUsername(String username, Pageable pageable);

    // 날짜 필터 있는 경우
    Page<LoginHistoryEntity> findByUsernameAndLoginAtBetween(String username, LocalDateTime start, LocalDateTime end, Pageable pageable);
	
	
    //********************************************************************************
    // admin 도메인
    //********************************************************************************
    
    List<LoginHistoryEntity> findByUsername(String username);

    @Query("SELECT l FROM LoginHistory l ORDER BY l.loginAt DESC")
    List<LoginHistoryEntity> findRecentLogins(Pageable pageable);

    // 최근 로그인 이력 조회 -> 리스크 서비스에서 위험을 계산 하기 위함
	List<LoginHistoryEntity> findTop20ByUsernameOrderByLoginAtDesc(String username);

    @Query("""
            select count(l)
            from LoginHistory l
            where l.username = :username
              and l.success = false
              and l.loginAt >= :since
            """)
    long countFailedLoginsSince(@Param("username") String username, @Param("since") LocalDateTime since);

    @Query("""
            select case when count(l) > 0 then true else false end
            from LoginHistory l
            where l.username = :username
              and l.success = true
              and l.ipAddress = :ipAddress
              and (:excludeId < 0 or l.id <> :excludeId)
            """)
    boolean existsSuccessfulLoginFromIp(
            @Param("username") String username,
            @Param("ipAddress") String ipAddress,
            @Param("excludeId") Long excludeId
    );

    @Query("""
            select case when count(l) > 0 then true else false end
            from LoginHistory l
            where l.username = :username
              and l.success = true
              and l.userAgent = :userAgent
              and (:excludeId < 0 or l.id <> :excludeId)
            """)
    boolean existsSuccessfulLoginFromUserAgent(
            @Param("username") String username,
            @Param("userAgent") String userAgent,
            @Param("excludeId") Long excludeId
    );

    @Query("""
            select count(l)
            from LoginHistory l
            where l.username = :username
              and l.success = true
              and (:excludeId < 0 or l.id <> :excludeId)
            """)
    long countSuccessfulLoginsBefore(@Param("username") String username, @Param("excludeId") Long excludeId);
	
}
