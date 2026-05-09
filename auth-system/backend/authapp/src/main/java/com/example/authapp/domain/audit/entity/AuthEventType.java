package com.example.authapp.domain.audit.entity;

public enum AuthEventType {
    SIGNUP_SUCCESS,			// 일반 회원가입
    SIGNUP_OAUTH2_SUCCESS,	// OAUTH2 회원가입
    
    LOGIN_SUCCESS,			// 로그인 성공
    LOGIN_FAIL,				// 로그인 실패
    LOGOUT,					// 사용자 로그아웃
    
    PASSWORD_CHANGE,		// 패스워드 변경
    PASSWORD_RESET,			// 패스워드 초기화
    
    TOKEN_REISSUE,			// 토큰 재발급
    
    ADMIN_FORCE_LOGOUT,     // 관리자 수동 조치
    SECURITY_FORCE_LOGOUT,  // 시스템 자동 차단

    ACCOUNT_PROFILE_UPDATED,// 계정 프로필 수정
    ACCOUNT_DEACTIVATED,	// 계정 소프트 탈퇴
    ACCOUNT_DELETE			// 계정 삭제

}
