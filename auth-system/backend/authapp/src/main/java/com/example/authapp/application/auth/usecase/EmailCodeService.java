package com.example.authapp.application.auth.usecase;

import java.time.LocalDateTime;
import java.util.Random;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.authapp.domain.notification.service.MailService;
import com.example.authapp.domain.user.exception.UserException;
import com.example.authapp.domain.user.service.UserQueryService;
import com.example.authapp.domain.verificatoin.dto.SendCodeRequest;
import com.example.authapp.domain.verificatoin.dto.VerifyCodeRequest;
import com.example.authapp.domain.verificatoin.entity.EmailCodeEntity;
import com.example.authapp.domain.verificatoin.entity.EmailCodePurpose;
import com.example.authapp.domain.verificatoin.exception.EmailCodeException;
import com.example.authapp.domain.verificatoin.repository.EmailCodeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class EmailCodeService {

    private final EmailCodeRepository emailCodeRepository;
    private final MailService mailService;
    private final UserQueryService userQueryService;

    // 인증 코드를 발송한다.
    public void sendCode(SendCodeRequest request) {
        validateSendRequest(request);

        // 최근 발송 이력이 있으면 재발송을 제한한다.
        emailCodeRepository.findTopByEmailAndPurposeOrderByIdDesc(request.email(), request.purpose())
                .ifPresent(code -> {
                    if (code.getCreatedAt().isAfter(LocalDateTime.now().minusSeconds(60))) {
                        throw EmailCodeException.tooManyRequests();
                    }
                });

        // 새 인증 코드를 저장한다.
        String code = createCode();
        EmailCodeEntity entity = EmailCodeEntity.builder()
                .email(request.email())
                .code(code)
                .purpose(request.purpose())
                .used(false)
                .expiredAt(LocalDateTime.now().plusMinutes(3))
                .build();
        emailCodeRepository.save(entity);

        mailService.send(request.email(), getTitle(request.purpose()), "인증코드 : " + code + "\n3분 안에 입력해주세요.");
    }

    // 인증 코드를 검증한다.
    public void verifyCode(VerifyCodeRequest request, boolean markAsUsed) {
        EmailCodeEntity entity = emailCodeRepository.findTopByEmailAndPurposeOrderByIdDesc(request.email(), request.purpose())
                .orElseThrow(EmailCodeException::codeNotFound);
        validate(entity, request.code());
        if (markAsUsed) {
            entity.use();
        }
    }

    // 발송 목적별 요청 조건을 검증한다.
    private void validateSendRequest(SendCodeRequest request) {
        switch (request.purpose()) {
            case SIGNUP -> {
                if (userQueryService.existsByEmail(request.email())) {
                    throw UserException.emailAlreadyExists();
                }
            }
            case RESET_PASSWORD -> userQueryService.getByUsernameAndEmail(request.username(), request.email());
            case FIND_USERNAME -> userQueryService.getByEmail(request.email());
        }
    }

    // 발송 목적별 메일 제목을 반환한다.
    private String getTitle(EmailCodePurpose purpose) {
        return switch (purpose) {
            case SIGNUP -> "[AuthApp] 회원가입 인증코드";
            case RESET_PASSWORD -> "[AuthApp] 비밀번호 재설정 인증코드";
            case FIND_USERNAME -> "[AuthApp] 아이디 찾기 인증코드";
        };
    }

    // 인증 코드 상태와 값을 검증한다.
    private void validate(EmailCodeEntity entity, String code) {
        if (entity.isUsed()) {
            throw EmailCodeException.alreadyUsed();
        }
        if (entity.isExpired()) {
            throw EmailCodeException.codeExpired();
        }
        if (!entity.getCode().equals(code)) {
            throw EmailCodeException.invalidCode();
        }
    }

    // 6자리 인증 코드를 생성한다.
    private String createCode() {
        return String.valueOf(100000 + new Random().nextInt(900000));
    }
}
