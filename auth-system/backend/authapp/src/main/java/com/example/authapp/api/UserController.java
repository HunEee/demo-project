package com.example.authapp.api;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.authapp.domain.user.dto.PasswordChangeRequest;
import com.example.authapp.domain.user.dto.UserRequestDTO;
import com.example.authapp.domain.user.dto.UserResponseDTO;
import com.example.authapp.domain.user.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    
    // 자체 로그인 유저 존재 확인
    @PostMapping(value = "/exist", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Boolean> existUserApi(@Validated(UserRequestDTO.existGroup.class) @RequestBody UserRequestDTO dto){
        return ResponseEntity.ok(userService.existUser(dto));
    }

    // 회원가입(email + verificationCode 포함)
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Long>> joinApi(@Validated(UserRequestDTO.addGroup.class) @RequestBody UserRequestDTO dto){
        Long id = userService.addUser(dto);
        Map<String, Long> responseBody = Collections.singletonMap("userEntityId", id);
        return ResponseEntity.status(201).body(responseBody);
    }
    
    // 모든 유저 조회 (관리자용)
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<UserResponseDTO>> getAllUsersApi() {
        List<UserResponseDTO> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    // 유저 정보
    @GetMapping(value = "/me", produces = MediaType.APPLICATION_JSON_VALUE)
    public UserResponseDTO userMe() {
        return userService.readUser();
    }

    // 유저 수정 (자체 로그인 유저만)
    @PutMapping(value = "/me", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Long> updateUser(
            @Validated(UserRequestDTO.updateGroup.class) @RequestBody UserRequestDTO dto) throws AccessDeniedException {
        return ResponseEntity.status(200).body(userService.updateUser(dto));
    }

    // 유저 제거 (자체/소셜)
    @DeleteMapping(value = "/me", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Boolean> deleteUser(
            @Validated(UserRequestDTO.deleteGroup.class) @RequestBody UserRequestDTO dto) throws AccessDeniedException {
        userService.deleteUser(dto);
        return ResponseEntity.status(200).body(true);
    }
    
    // 비밀번호 변경(로그인 상태)
    @PutMapping("/me/password")
    public void changePassword(
            @AuthenticationPrincipal(expression = "username") String username,
            @RequestBody PasswordChangeRequest request,
            HttpServletRequest httpRequest
    ) {
        String ip = httpRequest.getRemoteAddr();
        String userAgent = httpRequest.getHeader("User-Agent");
        userService.changePassword(username,request.getCurrentPassword(),request.getNewPassword(),ip, userAgent);
    }

    // 비밀번호 찾기(로그아웃 상태) -> username + email + verificationCode
    @PostMapping("/password-reset")
    public ResponseEntity<String> resetPassword(@RequestBody UserRequestDTO dto) {
        userService.resetPassword(dto);
        return ResponseEntity.ok("비밀번호 재설정 완료");
    }

    // 아이디 찾기(로그아웃 상태) -> email + verificationCode
    @PostMapping("/username-recovery")
    public ResponseEntity<Map<String, String>> findUsername(@RequestBody UserRequestDTO dto ) {
        String username = userService.findUsername(dto);
        return ResponseEntity.ok(Collections.singletonMap("username", username));
    }
    
    
}
