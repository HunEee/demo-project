package com.example.authapp.domain.jwt.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import com.example.authapp.domain.jwt.entity.RefreshTokenEntity;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, Long> {

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
	void deleteByCreatedAtBefore(LocalDateTime createdAt);
	
    Optional<RefreshTokenEntity> findByRefresh(String refresh);
    
    // 유저의 토큰들 조회
    List<RefreshTokenEntity> findByUsername(String username);
	
    
    // 세션 관리
    List<RefreshTokenEntity> findByUsernameAndRevokedFalse(String username);
    Optional<RefreshTokenEntity> findByIdAndUsername(Long id, String username);
}