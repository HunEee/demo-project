package com.example.authapp.domain.audit.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.authapp.domain.audit.entity.LoginHistory;
import com.example.authapp.domain.jwt.entity.RefreshEntity;

public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Long> {
	
	//Optional<RefreshEntity> findByRefresh(String refresh);
	
}
