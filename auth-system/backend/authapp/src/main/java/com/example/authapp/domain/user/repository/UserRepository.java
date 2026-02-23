package com.example.authapp.domain.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.authapp.domain.user.entity.UserEntity;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
	
	// 회원 가입시 이미 username이 존재하는지 중복 검증을 진행
	Boolean existsByUsername(String username);
	
	// 회원 정보 수정시 자체 로그인 여부, 잠김 여부를 확인
	Optional<UserEntity> findByUsernameAndIsLockAndIsSocial(String username, Boolean isLock, Boolean isSocial);
	

	
}