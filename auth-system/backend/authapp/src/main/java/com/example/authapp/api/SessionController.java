package com.example.authapp.api;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.authapp.domain.session.dto.SessionResponse;
import com.example.authapp.domain.session.service.SessionService;
import com.example.authapp.security.principal.CustomUserDetails;
import com.example.authapp.security.principal.UserPrincipal;
import com.example.authapp.util.JWTUtil;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/sessions")
public class SessionController {

    private final SessionService sessionService;

    // 세션 조회
    @GetMapping
    public List<SessionResponse> getSessions(@AuthenticationPrincipal UserPrincipal user, HttpServletRequest request) {
        return sessionService.getSessions(user.getUsername(), request);
    }

    // 개별 로그아웃
    @DeleteMapping("/{id}")
    public ResponseEntity <Void> logoutSession( @PathVariable("id") Long id, @AuthenticationPrincipal UserPrincipal user) {
        sessionService.logoutSession(id, user.getUsername());
        return ResponseEntity.ok().build();
    }


    // 전체 로그아웃
    @DeleteMapping
    public ResponseEntity<Void> logoutAll(@AuthenticationPrincipal UserPrincipal user, HttpServletRequest request) {
        sessionService.logoutAll(user.getUsername(), request);
        return ResponseEntity.ok().build();
    }
    

    
}