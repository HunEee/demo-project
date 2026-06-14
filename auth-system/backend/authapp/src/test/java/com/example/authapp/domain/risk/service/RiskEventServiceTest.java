package com.example.authapp.domain.risk.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.authapp.domain.audit.entity.LoginHistoryEntity;
import com.example.authapp.domain.risk.entity.RiskEventEntity;
import com.example.authapp.domain.risk.entity.RiskEventType;
import com.example.authapp.domain.risk.repository.RiskEventRepository;

@ExtendWith(MockitoExtension.class)
class RiskEventServiceTest {

    @Mock
    private RiskEventRepository riskEventRepository;

    @Test
    void loginRiskIsStoredAsLoginRiskNotTokenRisk() {
        RiskEventService service = new RiskEventService(riskEventRepository);
        LoginHistoryEntity history = LoginHistoryEntity.builder()
                .username("alice")
                .ipAddress("127.0.0.1")
                .userAgent("UA")
                .device("Chrome")
                .build();

        service.saveLoginRisk(history, 17, "LOGIN_FAIL;ABNORMAL_TIME;");

        ArgumentCaptor<RiskEventEntity> captor = ArgumentCaptor.forClass(RiskEventEntity.class);
        verify(riskEventRepository).save(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo(RiskEventType.LOGIN_RISK);
    }
}
