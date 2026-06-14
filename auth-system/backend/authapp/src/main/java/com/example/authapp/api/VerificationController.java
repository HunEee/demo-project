package com.example.authapp.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

import com.example.authapp.domain.verificatoin.dto.SendCodeRequest;
import com.example.authapp.domain.verificatoin.dto.VerifyCodeRequest;
import com.example.authapp.application.auth.usecase.VerificationUseCase;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/verification")
public class VerificationController {

    private final VerificationUseCase emailCodeService;
    
    // 인증코드를 발송한다.
    @PostMapping("/codes")
    public ResponseEntity<String> sendCode(@RequestBody SendCodeRequest request) {
        emailCodeService.sendCode(request);
        return ResponseEntity.ok("인증코드 발송 완료");
    }
    
    // 인증코드를 검증한다.
    @PostMapping("/verify")
    public ResponseEntity<String> verifyCode(@RequestBody VerifyCodeRequest request) {
        // 사전 확인 용도이므로 인증코드를 사용 처리하지 않는다.
        emailCodeService.verifyCode(request,false);
        return ResponseEntity.ok("인증 성공");
    }
    
}
