package com.example.authapp.domain.settings;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.example.authapp.domain.admin.settings.service.SecuritySettingsValidator;

class SecuritySettingsValidatorTest {

    @Test
    void rejectsInvalidRiskThresholdOrder() {
        assertThatThrownBy(() -> SecuritySettingsValidator.validateRiskThresholds(70, 60, 80))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("medium < high < critical");
    }

    @Test
    void rejectsWildcardCorsWithCredentials() {
        assertThatThrownBy(() -> SecuritySettingsValidator.validateCorsOrigin("*", true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Wildcard origin cannot be used with credentials");
    }
}
