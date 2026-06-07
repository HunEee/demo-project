package com.example.authapp.api;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.authapp.application.auth.AuthFacade;
import com.example.authapp.application.auth.dto.LoginResponseDTO;
import com.example.authapp.domain.mfa.dto.MfaMethodResponse;
import com.example.authapp.domain.mfa.dto.MfaVerifyRequest;
import com.example.authapp.domain.mfa.dto.TotpConfirmRequest;
import com.example.authapp.domain.mfa.dto.TotpSetupResponse;
import com.example.authapp.domain.mfa.service.MfaService;
import com.example.authapp.security.principal.UserPrincipal;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class MfaController {

    private final MfaService mfaService;
    private final AuthFacade authFacade;

    @PostMapping("/mfa/totp/setup")
    public TotpSetupResponse setupTotp(@AuthenticationPrincipal UserPrincipal principal) {
        return mfaService.setupTotp(principal.getUsername());
    }

    @PostMapping("/mfa/totp/confirm")
    public MfaMethodResponse confirmTotp(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody TotpConfirmRequest request
    ) {
        return mfaService.confirmTotp(principal.getUsername(), request);
    }

    @GetMapping("/mfa/methods")
    public List<MfaMethodResponse> methods(@AuthenticationPrincipal UserPrincipal principal) {
        return mfaService.methods(principal.getUsername());
    }

    @DeleteMapping("/mfa/methods/{id}")
    public ResponseEntity<Void> deleteMethod(@AuthenticationPrincipal UserPrincipal principal, @PathVariable("id") Long id) {
        mfaService.deleteMethod(principal.getUsername(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/auth/mfa/verify")
    public LoginResponseDTO verifyMfa(
            @RequestBody MfaVerifyRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response
    ) {
        return authFacade.completeMfaLogin(request, httpRequest, response);
    }
}
