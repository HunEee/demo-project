package com.example.authapp.domain.risk.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RiskEventEntityTest {

    @Test
    void builderStoresRuleBasedRiskEventFields() {
        RiskEventEntity event = RiskEventEntity.builder()
                .username("admin")
                .eventType(RiskEventType.ADMIN_LOGIN_FAILURE)
                .riskLevel(RiskLevel.HIGH)
                .score(60)
                .reason("ADMIN_LOGIN_FAILURE")
                .description("관리자 계정 로그인 실패")
                .ipAddress("10.0.0.10")
                .userAgent("JUnit-UA")
                .device("JUnit")
                .resolved(false)
                .build();

        assertThat(event.getEventType()).isEqualTo(RiskEventType.ADMIN_LOGIN_FAILURE);
        assertThat(event.getRiskLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(event.getDescription()).isEqualTo("관리자 계정 로그인 실패");
        assertThat(event.getUserAgent()).isEqualTo("JUnit-UA");
        assertThat(event.isResolved()).isFalse();
    }
}
