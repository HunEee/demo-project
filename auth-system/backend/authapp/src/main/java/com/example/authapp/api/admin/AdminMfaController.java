package com.example.authapp.api.admin;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.authapp.domain.mfa.dto.AdminMfaUserResponse;
import com.example.authapp.domain.mfa.dto.MfaExceptionRequest;
import com.example.authapp.domain.mfa.dto.MfaPolicyRequest;
import com.example.authapp.domain.mfa.dto.MfaPolicyResponse;
import com.example.authapp.domain.mfa.service.AdminMfaService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/mfa")
public class AdminMfaController {

    private final AdminMfaService adminMfaService;

    @GetMapping("/users")
    public List<AdminMfaUserResponse> users() {
        return adminMfaService.users();
    }

    @GetMapping("/users/{username}")
    public AdminMfaUserResponse user(@PathVariable("username") String username) {
        return adminMfaService.user(username);
    }

    @PostMapping("/users/{username}/reset")
    public ResponseEntity<Void> reset(
            @PathVariable("username") String username,
            @RequestBody(required = false) MfaExceptionRequest request,
            HttpServletRequest httpRequest
    ) {
        adminMfaService.reset(username, request != null ? request.reason() : null, httpRequest);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/users/{username}/exception")
    public ResponseEntity<Void> createException(
            @PathVariable("username") String username,
            @RequestBody MfaExceptionRequest request,
            HttpServletRequest httpRequest
    ) {
        adminMfaService.createException(username, request, httpRequest);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/users/{username}/exception")
    public ResponseEntity<Void> revokeException(@PathVariable("username") String username, HttpServletRequest request) {
        adminMfaService.revokeException(username, request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/policy")
    public MfaPolicyResponse policy() {
        return adminMfaService.policy();
    }

    @PatchMapping("/policy")
    public MfaPolicyResponse updatePolicy(@RequestBody MfaPolicyRequest request, HttpServletRequest httpRequest) {
        return adminMfaService.updatePolicy(request.policy(), httpRequest);
    }
}
