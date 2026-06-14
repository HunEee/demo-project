package com.example.authapp.application.auth.usecase;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.authapp.domain.notification.service.MailService;
import com.example.authapp.domain.verificatoin.entity.EmailTokenEntity;
import com.example.authapp.domain.verificatoin.entity.TokenPurpose;
import com.example.authapp.domain.verificatoin.repository.EmailTokenRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class EmailTokenService {

    private final EmailTokenRepository emailTokenRepository;
    private final MailService mailService;

    // 회원가입 이메일 인증 링크를 발송한다.
    public void sendSignupToken(String email) {
        String token = UUID.randomUUID().toString();

        EmailTokenEntity entity = EmailTokenEntity.builder()
                .email(email)
                .token(token)
                .purpose(TokenPurpose.SIGNUP)
                .used(false)
                .expiredAt(LocalDateTime.now().plusMinutes(30))
                .build();

        emailTokenRepository.save(entity);

        String link = "http://localhost:9090/api/email/verify/signup?token=" + token;
        mailService.send(email, "[AuthApp] 회원가입 이메일 인증", "아래 링크를 클릭하세요.\n" + link);
    }

    // 비밀번호 재설정 링크를 발송한다.
    public void sendResetPasswordToken(String email) {
        String token = UUID.randomUUID().toString();

        EmailTokenEntity entity = EmailTokenEntity.builder()
                .email(email)
                .token(token)
                .purpose(TokenPurpose.RESET_PASSWORD)
                .used(false)
                .expiredAt(LocalDateTime.now().plusMinutes(15))
                .build();

        emailTokenRepository.save(entity);

        String link = "http://localhost:9090/api/email/verify/reset?token=" + token;
        mailService.send(email, "[AuthApp] 비밀번호 재설정", "아래 링크를 클릭하세요.\n" + link);
    }

    // 회원가입 인증 토큰을 검증한다.
    public String verifySignupToken(String token) {
        EmailTokenEntity entity = findValidToken(token, TokenPurpose.SIGNUP);
        entity.use();
        return entity.getEmail();
    }

    // 비밀번호 재설정 토큰을 검증한다.
    public String verifyResetToken(String token) {
        EmailTokenEntity entity = findValidToken(token, TokenPurpose.RESET_PASSWORD);
        entity.use();
        return entity.getEmail();
    }

    // 토큰 존재 여부와 상태를 검증한다.
    private EmailTokenEntity findValidToken(String token, TokenPurpose purpose) {
        EmailTokenEntity entity = emailTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("토큰이 없습니다."));
        if (entity.isUsed()) {
            throw new RuntimeException("이미 사용된 토큰입니다.");
        }
        if (entity.isExpired()) {
            throw new RuntimeException("토큰이 만료되었습니다.");
        }
        if (entity.getPurpose() != purpose) {
            throw new RuntimeException("잘못된 토큰입니다.");
        }
        return entity;
    }
}
