package com.example.authapp.api;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.authapp.domain.audit.dto.SecurityStatusResponseDTO;
import com.example.authapp.domain.audit.service.SecurityDashboardService;
import com.example.authapp.security.principal.UserPrincipal;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/security")
@RequiredArgsConstructor
public class SecurityController {

    private final SecurityDashboardService securityService;

    @GetMapping
    public SecurityStatusResponseDTO getSecurityStatus(@AuthenticationPrincipal UserPrincipal user) {
        return securityService.getSecurityStatus(user.getUsername());
    }
    
    
}
