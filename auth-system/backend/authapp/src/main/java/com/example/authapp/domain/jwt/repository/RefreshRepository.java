package com.example.authapp.domain.jwt.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import com.example.authapp.domain.jwt.entity.RefreshEntity;

public interface RefreshRepository extends JpaRepository<RefreshEntity, Long> {

	// 리프레시 토큰이 존재하는지 여부
	Boolean existsByRefresh(String refreshToken);
	
	// JWT Refresh 토큰 기반 삭제
	@Transactional
	void deleteByRefresh(String refresh);
	
	// username 기반 삭제 -> 탈퇴시
	@Transactional
	void deleteByUsername(String username);
	
	// 특정일 지난 refresh 토큰 삭제
	@Transactional
	void deleteByCreatedDateBefore(LocalDateTime createdDate);
	
    Optional<RefreshEntity> findByRefresh(String refresh);
    
    // 유저의 토큰들 조회
    List<RefreshEntity> findByUsername(String username);
	
    
    // 세션 관리
    List<RefreshEntity> findByUsernameAndRevokedFalse(String username);
    Optional<RefreshEntity> findByIdAndUsername(Long id, String username);
}