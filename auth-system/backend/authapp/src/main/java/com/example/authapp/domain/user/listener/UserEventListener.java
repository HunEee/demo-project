package com.example.authapp.domain.user.listener;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.example.authapp.domain.audit.service.AuthEventLogService;
import com.example.authapp.domain.jwt.service.RefreshTokenService;
import com.example.authapp.domain.user.event.UserDeletedEvent;
import com.example.authapp.domain.user.event.UserProfileUpdatedEvent;
import com.example.authapp.domain.user.event.UserSignedUpEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserEventListener {

    private final AuthEventLogService authEventLogService;


    // 회원가입
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(UserSignedUpEvent event) {
        authEventLogService.signupSuccess(event.getUsername());
    }

    // 회원정보 수정
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(UserProfileUpdatedEvent event) {
        authEventLogService.accountProfileUpdated(event.getUsername());
    }

    // 회원 삭제
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(UserDeletedEvent event) {
        authEventLogService.accountDeactivated(event.getUsername());
    }
    
    
}
