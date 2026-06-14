package com.example.authapp.domain.user.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.authapp.domain.user.entity.SocialProviderType;
import com.example.authapp.domain.user.entity.UserEntity;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
	
	// 회원 가입시 이미 username이 존재하는지 중복 검증을 진행
	Boolean existsByUsername(String username);
	// 회원 가입시 이미 email이 존재하는지 중복 검증을 진행
	Boolean existsByEmail(String email);
	
	// 회원 정보 수정시 탈퇴 여부를 확인
	Optional<UserEntity> findByUsernameAndDeletedAtIsNull(String username);
	
	// 비밀번호 재설정 or 비밀번호 찾기 or username 찾기(탈퇴 상태가 아닌 경우)
	Optional<UserEntity> findByEmailAndDeletedAtIsNull(String email);
	Optional<UserEntity> findByUsernameAndEmailAndDeletedAtIsNull(String username, String email);

	
	void deleteByUsername(String username);
	
	
	// ROLE가 LAZY 로딩이라 한번에 조회
	@Query("SELECT u FROM UserEntity u JOIN FETCH u.roles WHERE u.username = :username AND u.locked = false")
	Optional<UserEntity> findWithRoles(@Param("username") String username);
	
	// 소셜 로그인 회원 존재 여부 확인
	Optional<UserEntity> findByUsernameAndSocial(String username, Boolean social);
	Optional<UserEntity> findBySocialProviderTypeAndProviderId(SocialProviderType socialProviderType, String providerId);
	
	
	// 자체/소셜 유저 정보 조회
	Optional<UserEntity> findByUsernameAndLocked(String username, Boolean locked);

	// 유저 정보 찾기
	Optional<UserEntity> findByUsername(String username);

	@EntityGraph(attributePaths = "roles")
	@Query("SELECT u FROM UserEntity u WHERE u.username = :username")
	Optional<UserEntity> findByUsernameWithRoles(@Param("username") String username);

	// ADMIN들 조회
    @Query("SELECT u FROM UserEntity u JOIN u.roles r WHERE r.name = 'ROLE_ADMIN'")
    List<UserEntity> findAllAdmins();




    
}
