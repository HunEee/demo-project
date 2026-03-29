package com.example.authapp.domain.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.example.authapp.domain.user.entity.UserEntity;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
	
	// 회원 가입시 이미 username이 존재하는지 중복 검증을 진행
	Boolean existsByUsername(String username);
	
	// 회원 가입시 이미 email이 존재하는지 중복 검증을 진행
	Boolean existsByEmail(String email);
	
	// 회원 정보 수정시 자체 로그인 여부, 잠김 여부를 확인
	Optional<UserEntity> findByUsernameAndLockedAndIsSocial(String username, Boolean Locked, Boolean isSocial);
	
	// ROLE가 LAZY 로딩이라 한번에 조회
	@Query("SELECT u FROM UserEntity u JOIN FETCH u.roles WHERE u.username = :username AND u.locked = false AND u.isSocial = false")
	Optional<UserEntity> findWithRoles(@Param("username") String username);
	
	// 소셜 로그인 회원 존재 여부 확인
	Optional<UserEntity> findByUsernameAndIsSocial(String username, Boolean isSocial);
	
	@Transactional
	void deleteByUsername(String username);
	
	// 자체/소셜 유저 정보 조회
	Optional<UserEntity> findByUsernameAndLocked(String username, Boolean locked);
	
}