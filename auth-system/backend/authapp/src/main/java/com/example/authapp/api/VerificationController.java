package com.example.authapp.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

import com.example.authapp.domain.verificatoin.service.EmailCodeService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/verification")
public class VerificationController {

    private final EmailCodeService emailCodeService;
    
    // 회원가입 인증코드 발송
    @PostMapping("/signup/send")
    public String sendSignupCode(@RequestParam String email) {
        emailCodeService.sendSignupCode(email);
        return "회원가입 인증코드 발송 완료";
    }
    
    // 비밀번호 찾기 코드 발송 (username + email 일치해야 발송)
    @PostMapping("/password/send")
    public ResponseEntity<String> sendResetPasswordCode(@RequestParam String username,@RequestParam String email) {
        emailCodeService.sendResetPasswordCode(username, email);
        return ResponseEntity.ok("비밀번호 재설정 인증코드 발송 완료");
    }

    // 아이디 찾기 코드 발송
    @PostMapping("/username/send")
    public ResponseEntity<String> sendFindUsernameCode(@RequestParam String email) {
        emailCodeService.sendFindUsernameCode(email);
        return ResponseEntity.ok("아이디 찾기 인증코드 발송 완료");
    }
    
    
}
