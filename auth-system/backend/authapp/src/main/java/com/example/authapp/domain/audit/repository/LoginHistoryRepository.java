package com.example.authapp.domain.audit.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.authapp.domain.audit.entity.LoginHistory;
import com.example.authapp.domain.jwt.entity.RefreshEntity;

public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Long> {
	
    //********************************************************************************
	// 로그인 기록 조회
    //********************************************************************************
	
    // 날짜 필터 없는 경우
	Page<LoginHistory> findByUsername(String username, Pageable pageable);

    // 날짜 필터 있는 경우
    Page<LoginHistory> findByUsernameAndLoginAtBetween(String username, LocalDateTime start, LocalDateTime end, Pageable pageable);
	
	
    //********************************************************************************
    // admin 도메인
    //********************************************************************************
    List<LoginHistory> findByUsername(String username);

    @Query("SELECT l FROM LoginHistory l ORDER BY l.loginAt DESC")
    List<LoginHistory> findRecentLogins(Pageable pageable);
	
}
