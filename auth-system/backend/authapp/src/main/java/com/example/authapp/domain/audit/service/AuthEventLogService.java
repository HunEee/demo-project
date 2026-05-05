package com.example.authapp.domain.audit.service;

import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.example.authapp.domain.audit.entity.AuthEventLogEntity;
import com.example.authapp.domain.audit.entity.AuthEventType;
import com.example.authapp.domain.audit.repository.AuthEventLogRepository;
import com.example.authapp.util.ClientUtil;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthEventLogService {

    private final AuthEventLogRepository repository;

    // ==================================================================================================================
    // 공통 저장
    // ==================================================================================================================
    public void save(String username,AuthEventType type,String description) {
        AuthEventLogEntity event = AuthEventLogEntity.builder()
                .username(username)
                .type(type)
                .description(description)
                .ipAddress(getIp())      
                .device(getDevice())     
                .build();

        repository.save(event);
    }

    // ==================================================================================================================
    // request 내부에서 직접 가져오기
    // ==================================================================================================================
    private HttpServletRequest getRequest() {
        ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attr != null ? attr.getRequest() : null;
    }

    private String getIp() {
        HttpServletRequest request = getRequest();
        return request != null ? ClientUtil.getIp(request) : "UNKNOWN";
    }

    private String getDevice() {
        HttpServletRequest request = getRequest();
        if (request == null) return "UNKNOWN";
        String ua = ClientUtil.getUserAgent(request);
        return ClientUtil.getDevice(ua);
    }
    
    
    //==================================================================================================================
    // 편의 메서드
    //==================================================================================================================

    // 일반 회원가입
	public void signupSuccess(String username) {      
		save(username, AuthEventType.SIGNUP_SUCCESS, "일반 회원가입 성공");
	}
	
	// OAuth2 회원가입
	public void signupOauth2Success(String username, String provider) {
        save(username, AuthEventType.SIGNUP_OAUTH2_SUCCESS, "OAuth2 회원가입 성공 (" + provider + ")");	
	}
	
	// 회원 수정
    public void accountProfileUpdated(String username) {
        save(username, AuthEventType.ACCOUNT_PROFILE_UPDATED, "회원 정보 수정");
    }
    
    // 회원 탈퇴
    public void accountDeactivated(String username) {
        save(username, AuthEventType.ACCOUNT_DEACTIVATED, "회원 탈퇴 처리됨");
    }
	
	
	//*********************************************************************************************************************
    public void loginSuccess(String username) {
        save(username, AuthEventType.LOGIN_SUCCESS, "로그인 성공");
    }
    
    public void loginFail(String username, String reason) {
        save(username, AuthEventType.LOGIN_FAIL, "로그인 실패: " + reason);
    }

    public void logout(String username) {
        save(username, AuthEventType.LOGOUT, "로그아웃");
    }
    
    //*********************************************************************************************************************
    public void passwordChange(String username) {
        save(username, AuthEventType.PASSWORD_CHANGE, "비밀번호 변경");
    }

    public void passwordReset(String username) {
        save(username, AuthEventType.PASSWORD_RESET, "비밀번호 초기화/변경");
    }
    
    //*********************************************************************************************************************
    public void tokenReissue(String username) {
        save(username, AuthEventType.TOKEN_REISSUE, "토큰 재발급");
    }
    
    //*********************************************************************************************************************
    public void adminForceLogout(String username) {
        save(username, AuthEventType.ADMIN_FORCE_LOGOUT, "관리자 강제 로그아웃");
    }

    public void securityForceLogout(String username) {
        save(username, AuthEventType.SECURITY_FORCE_LOGOUT, "보안 정책 강제 로그아웃");
    }

    //*********************************************************************************************************************




    

}