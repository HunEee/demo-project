package com.example.authapp.application.auth.usecase;

import org.springframework.stereotype.Service;

import com.example.authapp.domain.verificatoin.dto.SendCodeRequest;
import com.example.authapp.domain.verificatoin.dto.VerifyCodeRequest;
import com.example.authapp.application.auth.usecase.EmailCodeService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VerificationUseCase {

    private final EmailCodeService emailCodeService;

    public void sendCode(SendCodeRequest request) {
        emailCodeService.sendCode(request);
    }

    public void verifyCode(VerifyCodeRequest request, boolean removeAfterVerification) {
        emailCodeService.verifyCode(request, removeAfterVerification);
    }
}

