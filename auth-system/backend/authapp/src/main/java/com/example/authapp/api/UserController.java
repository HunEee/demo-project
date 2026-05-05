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

import com.example.authapp.domain.user.dto.UserResponse;
import com.example.authapp.domain.user.dto.password.ChangePasswordRequest;
import com.example.authapp.domain.user.dto.password.ResetPasswordRequest;
import com.example.authapp.domain.user.dto.user.CheckUsernameRequest;
import com.example.authapp.domain.user.dto.user.FindUsernameRequest;
import com.example.authapp.domain.user.dto.user.SignupRequest;
import com.example.authapp.domain.user.dto.user.UpdateUserRequest;
import com.example.authapp.domain.user.service.PasswordService;
import com.example.authapp.domain.user.service.UserCommandService;
import com.example.authapp.domain.user.service.UserQueryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserCommandService userCommandService;
    private final UserQueryService userQueryService;
    private final PasswordService passwordService;
    
    // 회원가입(email + verificationCode 포함)
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Long> signup(@Valid @RequestBody SignupRequest request){
        return ResponseEntity.status(201).body(userCommandService.addUser(request));
    }
    
    // 유저 수정 (자체 로그인 유저만)
    @PatchMapping(value = "/me", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Long> updateUser(        
    		@AuthenticationPrincipal(expression = "username") String username,
            @RequestBody UpdateUserRequest request
    ) {
        return ResponseEntity.status(200).body(userCommandService.updateUser(username, request));
    }

    // 유저 제거 
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMyAccount(@AuthenticationPrincipal(expression = "username") String username) {
        userCommandService.deleteMyAccount(username);
        return ResponseEntity.noContent().build(); //204
    }
    
    //*******************************************************************************************************************************
    //*******************************************************************************************************************************
    
    // 자체 로그인 유저 존재 확인
    @PostMapping(value = "/exists", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Boolean> checkUserExists(@Valid @RequestBody CheckUsernameRequest request){
        return ResponseEntity.ok(userQueryService.existUsername(request.username()));
    }

    // 모든 유저 조회 (관리자용)
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> users = userQueryService.getAllUsers();
        return ResponseEntity.ok(users);
    }
    
    // 유저 정보
    @GetMapping(value = "/me", produces = MediaType.APPLICATION_JSON_VALUE)
    public UserResponse userMe() {
        return userQueryService.readUser();
    }

    // 아이디 찾기(로그아웃 상태) -> email + verificationCode
    @PostMapping("/find-username")
    public ResponseEntity<Map<String, String>> findUsername(@Valid @RequestBody FindUsernameRequest request) {
        String username = userCommandService.findUsername(request);
        return ResponseEntity.ok(Collections.singletonMap("username", username));
    }
    
    //*******************************************************************************************************************************
    //*******************************************************************************************************************************
    
    // 비밀번호 변경(로그인 상태)
    @PutMapping("/me/password")
    public void changePassword(
            @AuthenticationPrincipal(expression = "username") String username,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        passwordService.changePassword(username,request.currentPassword(),request.newPassword());
    }

    // 비밀번호 찾기(로그아웃 상태) -> username + email + verificationCode
    @PostMapping("/password/reset")
    public ResponseEntity<String> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        passwordService.resetPassword(request.username(),request.email(),request.verificationCode(),request.newPassword());
        return ResponseEntity.ok("비밀번호 재설정 완료");
    }

    
}
