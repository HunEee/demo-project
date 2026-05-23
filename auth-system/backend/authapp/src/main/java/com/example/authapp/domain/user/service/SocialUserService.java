package com.example.authapp.domain.user.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.authapp.domain.audit.service.AuthEventLogService;
import com.example.authapp.domain.user.entity.RoleEntity;
import com.example.authapp.domain.user.entity.SocialProviderType;
import com.example.authapp.domain.user.entity.UserEntity;
import com.example.authapp.domain.user.exception.UserException;
import com.example.authapp.domain.user.repository.RoleRepository;
import com.example.authapp.domain.user.repository.UserRepository;
import com.example.authapp.security.oauth2.provider.OAuth2UserInfo;

import lombok.RequiredArgsConstructor;

/**
 * 소셜 로그인 사용자 처리 서비스
 * - OAuth2 로그인 사용자 조회
 * - 기존 사용자면 검증 후 반환
 * - 없으면 신규 회원 생성
 * - 일반 회원 이메일과 동일하면 소셜 계정 연결
 *
 * - 지원 예: GOOGLE, KAKAO, NAVER
 */
@Service
@RequiredArgsConstructor
@Transactional
public class SocialUserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuthEventLogService authEventLogService;

    
    /**
     * 소셜 사용자 조회 또는 생성
     * 1. provider + providerId 로 기존 소셜 회원 조회
     * 2. 있으면 검증 후 반환
     * 3. 없으면 email 기준 기존 일반 회원 조회
     * 4. 이메일이 같으면 소셜 계정 연결
     * 5. 없으면 신규 소셜 회원 생성
     */
    public UserEntity loadOrCreate(OAuth2UserInfo info) {

        // GOOGLE, KAKAO 같은 provider 문자열 -> Enum 변환
        SocialProviderType providerType = SocialProviderType.valueOf(info.provider());

        return userRepository
                // provider + providerId 기준 기존 소셜 회원 조회
                .findBySocialProviderTypeAndProviderId(providerType,info.providerId())
                // 기존 회원이면 검증 후 반환
                .map(user -> validateAndReturnExistingSocialUser(user, info))
                // 없으면 이메일 기준 기존 회원 조회 또는 신규 생성
                .orElseGet(() -> loadByEmailOrCreate(providerType, info));
    }

    /**
     * 기존 소셜 회원 검증 후 반환
     * - 탈퇴 여부, 잠금 여부, 활성화 여부
     * - provider/providerId 일치 여부
     */
    private UserEntity validateAndReturnExistingSocialUser(UserEntity user, OAuth2UserInfo info) {
        // 계정 사용 가능 여부 검증
        validateAvailable(user);
        // 현재 로그인 provider와 기존 provider 일치 여부 검증
        validateProvider(user, info);
        return user;
    }

    /**
     * 이메일 기준 기존 회원 조회 후 처리
     * - 이메일 동일한 회원 존재 → 소셜 계정 연결
     * - 이메일 동일 회원 없음 → 신규 회원 생성
     */
    private UserEntity loadByEmailOrCreate(SocialProviderType providerType, OAuth2UserInfo info) {

        return userRepository
                // 이메일 기준 기존 회원 조회
                .findByEmailAndDeletedAtIsNull(info.email())
                // 기존 회원 있으면 소셜 연결
                .map(user -> linkSocialAccount(user, providerType, info))
                // 없으면 신규 소셜 회원 생성
                .orElseGet(() -> createSocialUser(info));
    }

    /**
     * 기존 일반 회원에 소셜 계정 연결
     * - 기존 일반 로그인 회원 존재
     * - 동일 이메일로 Google 로그인 → 기존 계정에 Google 연결
     */
    private UserEntity linkSocialAccount(UserEntity user, SocialProviderType providerType, OAuth2UserInfo info) {
        // 계정 사용 가능 여부 확인
        validateAvailable(user);
        // 이미 다른 소셜 계정 연결되어 있는지 확인
        validateSocialLinkAvailable(user, info);
        // 소셜 계정 연결
        user.linkSocialAccount(providerType, info.providerId());
        return user;
    }

    /**
     * 신규 소셜 회원 생성
     * - username 중복 확인
     * - ROLE_USER 부여
     * - 소셜 회원 생성
     * - OAuth 회원가입 로그 기록
     */
    private UserEntity createSocialUser(OAuth2UserInfo info) {

        // username 중복 확인
        userRepository.findByUsername(info.username())
                .ifPresent(user -> {
                    throw UserException.usernameAlreadyExists();
                });

        // 기본 사용자 권한 조회
        RoleEntity role = roleRepository.findByName("ROLE_USER")
                .orElseThrow(UserException::roleNotFound);

        // 신규 소셜 회원 생성
        UserEntity user = UserEntity.builder()
                // 소셜 provider 에서 가져온 정보 저장
                .username(info.username())
                .password("") // 소셜 로그인은 비밀번호 사용 안함
                .email(info.email())
                .nickname(info.nickname())
                // 계정 상태
                .enabled(true)
                .locked(false)
                // 소셜 회원 여부
                .social(true)
                // provider 정보
                .providerId(info.providerId())
                .socialProviderType(SocialProviderType.valueOf(info.provider()))
                .build();

        // 기본 권한 추가
        user.addRole(role);

        // DB 저장
        userRepository.save(user);

        // OAuth 회원가입 성공 로그 기록
        authEventLogService.signupOauth2Success(info.username(), info.provider());

        return user;
    }

    /**
     * 계정 사용 가능 여부 검증
     * - 삭제된 계정인지
     * - 잠긴 계정인지
     * - 비활성화 계정인지
     */
    private void validateAvailable(UserEntity user) {
        if (user.isDeleted()|| user.isLocked()|| !user.isEnabled()) {
            throw UserException.accountUnavailable();
        }
    }

    /**
     * provider/providerId 검증
     * - 기존 Google 회원인데
     * - Kakao 로 로그인 시도 → 충돌 예외 발생
     */
    private void validateProvider(UserEntity user, OAuth2UserInfo info) {
        if (
                !user.isSocial()
                // provider 다름
                || user.getSocialProviderType()
                != SocialProviderType.valueOf(info.provider())
                // providerId 다름
                || !info.providerId().equals(user.getProviderId())
        ) {

            throw UserException.socialAccountConflict();
        }
    }

    /**
     * 소셜 계정 연결 가능 여부 검증
     * - 일반 회원이면: 연결 가능
     * - 이미 소셜 회원이면: provider/providerId 검증 수행
     */
    private void validateSocialLinkAvailable(UserEntity user, OAuth2UserInfo info) {
        // 일반 회원이면 연결 가능
        if (!user.isSocial()) {
            return;
        }
        // 이미 소셜 회원이면 provider 검증
        validateProvider(user, info);
    }
    

}