package com.example.authapp.domain.verificatoin.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.authapp.domain.notification.service.MailService;
import com.example.authapp.domain.user.entity.UserEntity;
import com.example.authapp.domain.user.exception.UserException;
import com.example.authapp.domain.user.repository.UserRepository;
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
    private final UserRepository userRepository;
    
    
    /*
    ==============================================
    회원가입 이메일 인증 링크 발송
    ==============================================
    */
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

    /*
    ==============================================
    비밀번호 재설정 링크 발송
    ==============================================
    */
    public void sendResetPasswordToken(String email) {

        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(UserException::userNotFound);

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

    /*
    ==============================================
    회원가입 토큰 검증
    ==============================================
    */
    public String verifySignupToken(String token) {

        EmailTokenEntity entity = emailTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("토큰 없음"));

        if (entity.isUsed()) throw new RuntimeException("이미 사용됨");
        if (entity.isExpired()) throw new RuntimeException("토큰 만료");
        if (entity.getPurpose() != TokenPurpose.SIGNUP)
            throw new RuntimeException("잘못된 토큰");

        entity.use();

        return entity.getEmail();
    }

    /*
    ==============================================
    비밀번호 재설정 토큰 검증
    ==============================================
    */
    public String verifyResetToken(String token) {

        EmailTokenEntity entity = emailTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("토큰 없음"));

        if (entity.isUsed()) throw new RuntimeException("이미 사용됨");
        if (entity.isExpired()) throw new RuntimeException("토큰 만료");
        if (entity.getPurpose() != TokenPurpose.RESET_PASSWORD)
            throw new RuntimeException("잘못된 토큰");

        entity.use();

        return entity.getEmail();
    }
    
    
    
	
}
