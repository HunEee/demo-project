package com.example.authapp.domain.audit.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.authapp.domain.audit.entity.LoginHistory;
import com.example.authapp.domain.jwt.entity.RefreshEntity;

public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Long> {
	
	//Optional<RefreshEntity> findByRefresh(String refresh);
	
    //********************************************************************************
    // admin 도메인
    //********************************************************************************
    List<LoginHistory> findByUsername(String username);

    @Query("SELECT l FROM LoginHistory l ORDER BY l.loginAt DESC")
    List<LoginHistory> findRecentLogins(Pageable pageable);
	
}
