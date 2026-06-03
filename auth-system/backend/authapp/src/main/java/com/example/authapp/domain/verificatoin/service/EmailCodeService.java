package com.example.authapp.domain.verificatoin.service;

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
    
    // 통합 발송
    public void sendCode(SendCodeRequest request) {
    	// 코드 발송 목표별 분기
        validateSendRequest(request);
        // 재발송 제한 (1분)
        emailCodeRepository.findTopByEmailAndPurposeOrderByIdDesc(request.email(), request.purpose())
        	.ifPresent(code -> {
	            if (code.getCreatedAt().isAfter(LocalDateTime.now().minusSeconds(60))) {
	                throw EmailCodeException.tooManyRequests();
	            }
        	}
        );
        // 코드 생성
        String code = createCode();
        // 코드 저장
        EmailCodeEntity entity = EmailCodeEntity.builder()
                .email(request.email())
                .code(code)
                .purpose(request.purpose())
                .used(false)
                .expiredAt(LocalDateTime.now().plusMinutes(3))
                .build();
        emailCodeRepository.save(entity);
        // 코드 발송
        mailService.send(request.email(),getTitle(request.purpose()),"인증코드 : " + code + "\n3분 내 입력해주세요.");
    }

    // 통합 검증
    public void verifyCode(VerifyCodeRequest request, boolean markAsUsed) {
    	// 최근 발송한 코드 가져오기
        EmailCodeEntity entity = emailCodeRepository.findTopByEmailAndPurposeOrderByIdDesc(request.email(), request.purpose())
                	.orElseThrow(EmailCodeException::codeNotFound);
        // 검증
        validate(entity, request.code());
        // 분기 처리 -> 이메일 사전 검증 시에는 false로 하고 최종 제출할때 true처리
        if (markAsUsed) {
            entity.use(); // 👉 필요할 때만 사용 처리
        }
    }

    // =============================================================================================================================
    // 내부 로직
    // =============================================================================================================================

    private void validateSendRequest(SendCodeRequest request) {
        switch (request.purpose()) {
            case SIGNUP -> {
                if (userQueryService.existsByEmail(request.email())) {
                    throw UserException.emailAlreadyExists();
                }
            }
            case RESET_PASSWORD -> {
            	userQueryService.getByUsernameAndEmail(request.username(), request.email());
            }
            case FIND_USERNAME -> {
            	userQueryService.getByEmail(request.email());
            }
        }
    }

    private String getTitle(EmailCodePurpose purpose) {
        return switch (purpose) {
            case SIGNUP -> "[AuthApp] 회원가입 인증코드";
            case RESET_PASSWORD -> "[AuthApp] 비밀번호 재설정 인증코드";
            case FIND_USERNAME -> "[AuthApp] 아이디 찾기 인증코드";
        };
    }

    private void validate(EmailCodeEntity entity, String code) {
        if (entity.isUsed()) throw EmailCodeException.alreadyUsed();
        if (entity.isExpired()) throw EmailCodeException.codeExpired();
        if (!entity.getCode().equals(code)) throw EmailCodeException.invalidCode();
    }

    private String createCode() {
        return String.valueOf(100000 + new Random().nextInt(900000));
    }
    
}
