package com.example.authapp.api;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.authapp.application.user.dto.FindUsernameRequest;
import com.example.authapp.application.user.dto.SignupRequest;
import com.example.authapp.application.user.dto.UpdateUserProfileRequest;
import com.example.authapp.application.user.usecase.UserFacade;
import com.example.authapp.domain.user.dto.UserResponse;
import com.example.authapp.domain.user.dto.password.ChangePasswordRequest;
import com.example.authapp.domain.user.dto.password.ResetPasswordRequest;
import com.example.authapp.domain.user.dto.user.CheckUsernameRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserFacade userFacade; 
    
    // 이메일 인증코드를 포함해 회원가입을 처리한다.
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Long> signup(@Valid @RequestBody SignupRequest request){
        return ResponseEntity.status(201).body(userFacade.signup(request));
    }
    
    // 내 프로필 정보를 수정한다.
    @PatchMapping(value = "/me", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Long> updateUser(        
    		@AuthenticationPrincipal(expression = "username") String username,
            @RequestBody UpdateUserProfileRequest request
    ) {
        return ResponseEntity.status(200).body(userFacade.updateMyProfile(username, request));
    }

    // 내 계정을 삭제한다.
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMyAccount(@AuthenticationPrincipal(expression = "username") String username) {
        userFacade.deleteMyAccount(username);
        return ResponseEntity.noContent().build();
    }
    
    // 사용자명 존재 여부를 확인한다.
    @PostMapping(value = "/exists", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Boolean> checkUserExists(@Valid @RequestBody CheckUsernameRequest request){
        return ResponseEntity.ok(userFacade.existsUsername(request));
    }

    // 전체 사용자 목록을 조회한다.
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> users = userFacade.getAllUsers();
        return ResponseEntity.ok(users);
    }
    
    // 내 사용자 정보를 조회한다.
    @GetMapping(value = "/me", produces = MediaType.APPLICATION_JSON_VALUE)
    public UserResponse me(@AuthenticationPrincipal(expression = "username") String username) {
        return userFacade.getMyInfo(username);
    }
    
    // 이메일 인증코드로 사용자명을 찾는다.
    @PostMapping("/find-username")
    public ResponseEntity<Map<String, String>> findUsername(@Valid @RequestBody FindUsernameRequest request) {
        String username = userFacade.findUsername(request);
        return ResponseEntity.ok(Collections.singletonMap("username", username));
    }
    
    // 로그인 사용자의 비밀번호를 변경한다.
    @PutMapping("/me/password")
    public void changePassword(
            @AuthenticationPrincipal(expression = "username") String username,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        userFacade.changePassword(username, request.currentPassword(), request.newPassword());
    }

    // 이메일 인증코드로 비밀번호를 초기화한다.
    @PostMapping("/password/reset")
    public ResponseEntity<String> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        userFacade.resetPassword(request.username(), request.email(), request.verificationCode(), request.newPassword());
        return ResponseEntity.ok("비밀번호 재설정 완료");
    }

    
}
