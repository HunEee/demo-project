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
import com.example.authapp.domain.verificatoin.service.EmailCodeService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/verification")
public class VerificationController {

    private final EmailCodeService emailCodeService;
    
    // 인증코드 발송 (통합)
    @PostMapping("/codes")
    public ResponseEntity<String> sendCode(@RequestBody SendCodeRequest request) {
        emailCodeService.sendCode(request);
        return ResponseEntity.ok("인증코드 발송 완료");
    }
    
    // 인증코드 검증 (통합)
    @PostMapping("/verify")
    public ResponseEntity<String> verifyCode(@RequestBody VerifyCodeRequest request) {
    	// 사전에 코드가 맞는지 검증이라 사용처리는 두번째 파라미터 false로 넘긴다.
        emailCodeService.verifyCode(request,false);
        return ResponseEntity.ok("인증 성공");
    }
    
}
