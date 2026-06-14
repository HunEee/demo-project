package com.example.authapp.application.auth.usecase;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.authapp.domain.mfa.dto.MfaMethodResponse;
import com.example.authapp.domain.mfa.dto.PreAuthTotpSetupResponse;
import com.example.authapp.domain.mfa.dto.TotpConfirmRequest;
import com.example.authapp.domain.mfa.dto.TotpSetupResponse;
import com.example.authapp.application.auth.usecase.MfaService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MfaUseCase {

    private final MfaService mfaService;

    public TotpSetupResponse setupTotp(String username) {
        return mfaService.setupTotp(username);
    }

    public MfaMethodResponse confirmTotp(String username, TotpConfirmRequest request) {
        return mfaService.confirmTotp(username, request);
    }

    public List<MfaMethodResponse> methods(String username) {
        return mfaService.methods(username);
    }

    public void deleteMethod(String username, Long id, String code) {
        mfaService.deleteMethod(username, id, code);
    }

    public PreAuthTotpSetupResponse setupPreAuthTotpRegistration(String challengeId, String ipAddress, String userAgent) {
        return mfaService.setupPreAuthTotpRegistration(challengeId, ipAddress, userAgent);
    }
}

