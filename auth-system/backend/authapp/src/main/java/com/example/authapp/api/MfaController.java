package com.example.authapp.api;

import java.util.List;
import java.util.Map;

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
import com.example.authapp.domain.mfa.dto.MfaMethodDeleteRequest;
import com.example.authapp.domain.mfa.dto.MfaMethodResponse;
import com.example.authapp.domain.mfa.dto.MfaVerifyRequest;
import com.example.authapp.domain.mfa.dto.PreAuthTotpConfirmRequest;
import com.example.authapp.domain.mfa.dto.PreAuthTotpSetupResponse;
import com.example.authapp.domain.mfa.dto.TotpConfirmRequest;
import com.example.authapp.domain.mfa.dto.TotpSetupResponse;
import com.example.authapp.application.auth.usecase.MfaUseCase;
import com.example.authapp.security.principal.UserPrincipal;
import com.example.authapp.util.ClientUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class MfaController {

    private final MfaUseCase mfaService;
    private final AuthFacade authFacade;

    // 로그인 후 TOTP 등록 정보를 생성한다.
    @PostMapping("/mfa/totp/setup")
    public TotpSetupResponse setupTotp(@AuthenticationPrincipal UserPrincipal principal) {
        return mfaService.setupTotp(principal.getUsername());
    }

    // 로그인 후 TOTP 등록을 확정한다.
    @PostMapping("/mfa/totp/confirm")
    public MfaMethodResponse confirmTotp(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody TotpConfirmRequest request
    ) {
        return mfaService.confirmTotp(principal.getUsername(), request);
    }

    // 내 MFA 수단 목록을 조회한다.
    @GetMapping("/mfa/methods")
    public List<MfaMethodResponse> methods(@AuthenticationPrincipal UserPrincipal principal) {
        return mfaService.methods(principal.getUsername());
    }

    // MFA 수단을 삭제한다.
    @DeleteMapping("/mfa/methods/{id}")
    public ResponseEntity<Void> deleteMethod(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("id") Long id,
            @RequestBody MfaMethodDeleteRequest request
    ) {
        mfaService.deleteMethod(principal.getUsername(), id, request.code());
        return ResponseEntity.noContent().build();
    }

    // 로그인 전 TOTP 등록 정보를 생성한다.
    @PostMapping("/auth/mfa/totp/setup")
    public PreAuthTotpSetupResponse setupPreAuthTotp(
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest
    ) {
        return mfaService.setupPreAuthTotpRegistration(
                request.get("challengeId"),
                ClientUtil.getIp(httpRequest),
                ClientUtil.getUserAgent(httpRequest)
        );
    }

    // 로그인 전 TOTP 등록을 확정하고 JWT를 발급한다.
    @PostMapping("/auth/mfa/totp/confirm")
    public LoginResponseDTO confirmPreAuthTotp(
            @RequestBody PreAuthTotpConfirmRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response
    ) {
        return authFacade.completeMfaRegistration(request, httpRequest, response);
    }

    // MFA challenge를 검증하고 JWT를 발급한다.
    @PostMapping("/auth/mfa/verify")
    public LoginResponseDTO verifyMfa(
            @RequestBody MfaVerifyRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response
    ) {
        return authFacade.completeMfaLogin(request, httpRequest, response);
    }
}
