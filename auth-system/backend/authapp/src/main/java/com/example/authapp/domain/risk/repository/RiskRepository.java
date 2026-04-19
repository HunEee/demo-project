package com.example.authapp.domain.risk.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.authapp.domain.risk.entity.RiskEntity;

public interface RiskRepository extends JpaRepository<RiskEntity, Long> {

	// RiskEntity에 username 필드가 없어서 User에 Username을 가져옴
	Optional<RiskEntity> findByUserUsername(String username);
	
	


    
}
