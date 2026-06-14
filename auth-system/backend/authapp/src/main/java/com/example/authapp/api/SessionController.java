package com.example.authapp.api;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.authapp.domain.session.dto.SessionResponse;
import com.example.authapp.application.session.usecase.SessionUseCase;
import com.example.authapp.security.principal.UserPrincipal;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("${api.prefix}")
public class SessionController {

    private final SessionUseCase sessionService;

    // 내 활성 세션 목록을 조회한다.
    @GetMapping({"/sessions", "/me/sessions"})
    public List<SessionResponse> getSessions(@AuthenticationPrincipal UserPrincipal user, HttpServletRequest request) {
        return sessionService.getSessions(user.getUsername(), request);
    }

    // 지정한 세션을 로그아웃한다.
    @DeleteMapping({"/sessions/{id}", "/me/sessions/{id}"})
    public ResponseEntity <Void> logoutSession( @PathVariable("id") Long id, @AuthenticationPrincipal UserPrincipal user) {
        sessionService.logoutSession(id, user.getUsername());
        return ResponseEntity.ok().build();
    }


    // 현재 세션을 제외한 다른 세션을 로그아웃한다.
    @DeleteMapping("/sessions")
    public ResponseEntity<Void> logoutAll(@AuthenticationPrincipal UserPrincipal user, HttpServletRequest request) {
        sessionService.logoutAll(user.getUsername(), request);
        return ResponseEntity.ok().build();
    }
    

    
}
