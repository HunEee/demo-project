package com.example.authapp.domain.verificatoin.service;

import java.time.LocalDateTime;
import java.util.Random;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.authapp.domain.notification.service.MailService;
import com.example.authapp.domain.user.entity.UserEntity;
import com.example.authapp.domain.user.exception.UserException;
import com.example.authapp.domain.user.repository.UserRepository;
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
    private final UserRepository userRepository;
    
    // 회원가입 인증코드 발송
    public void sendSignupCode(String email) {
    	// 코드 생성
        String code = createCode();
        // 코드 저장
        EmailCodeEntity entity = EmailCodeEntity.builder()
                .email(email)
                .code(code)
                .purpose(EmailCodePurpose.SIGNUP)
                .used(false)
                .expiredAt(LocalDateTime.now().plusMinutes(5))
                .build();
        emailCodeRepository.save(entity);
        // 메일 발송
        mailService.send(email,"[AuthApp] 회원가입 인증코드", "인증코드 : " + code + "\n5분 내 입력해주세요.");  
    }

    // 회원가입 인증코드 검증
    public void verifySignupCode(String email, String code) {
        EmailCodeEntity entity = emailCodeRepository.findTopByEmailAndPurposeOrderByIdDesc(email, EmailCodePurpose.SIGNUP)
                        .orElseThrow(EmailCodeException::codeNotFound);
        // 검증
        validate(entity, code);
        // 코드 사용으로 바꾸기
        entity.use();
    }
    
    // 비밀번호 찾기 인증코드 발송
    public void sendResetPasswordCode(String username, String email) {
        userRepository.findByUsernameAndEmail(username, email).orElseThrow(UserException::userNotFound);
        saveAndSend(email, EmailCodePurpose.RESET_PASSWORD,"[AuthApp] 비밀번호 재설정 인증코드");
    }
    
    // 비밀번호 찾기 검증
    public void verifyResetPasswordCode(String email, String code) {
        EmailCodeEntity entity = emailCodeRepository.findTopByEmailAndPurposeOrderByIdDesc(email, EmailCodePurpose.RESET_PASSWORD)
        		.orElseThrow(EmailCodeException::codeNotFound);
        // 검증
        validate(entity, code);
        // 코드 사용 처리
        entity.use();
    }
    
    // username 찾기 인증코드 발송
    public void sendFindUsernameCode(String email) {
        userRepository.findByEmail(email).orElseThrow(UserException::userNotFound);
        saveAndSend(email, EmailCodePurpose.FIND_USERNAME,"[AuthApp] 아이디 찾기 인증코드");
    }
    
    // username 찾기 검증
    public void verifyFindUsernameCode(String email, String code) {
        EmailCodeEntity entity = emailCodeRepository.findTopByEmailAndPurposeOrderByIdDesc(email, EmailCodePurpose.FIND_USERNAME)
                .orElseThrow(EmailCodeException::codeNotFound);
        // 검증
        validate(entity, code);
        // 코드 사용 처리
        entity.use();
    }
    
    //==========================================
    // 내부 메서드
    // ==========================================
    private void saveAndSend(String email,EmailCodePurpose purpose,String title) {
    	// 코드 생성
        String code = createCode();
        // 코드 저장
        EmailCodeEntity entity = EmailCodeEntity.builder()
                .email(email)
                .code(code)
                .purpose(purpose)
                .used(false)
                .expiredAt(LocalDateTime.now().plusMinutes(5))
                .build();
        emailCodeRepository.save(entity);
        // 메일 발송
        mailService.send(email, title, "인증코드 : " + code + "\n5분 내 입력해주세요.");
    }
    
    private void validate(EmailCodeEntity entity, String code) {
    	// 사용 여부 -> 만료 여부 -> 코드 일치 여부
        if (entity.isUsed()) throw EmailCodeException.alreadyUsed();
        if (entity.isExpired()) throw EmailCodeException.codeExpired();
        if (!entity.getCode().equals(code)) throw EmailCodeException.invalidCode();
    }
    
    private String createCode() {
        return String.valueOf(100000 + new Random().nextInt(900000));
    }
    
	
}
