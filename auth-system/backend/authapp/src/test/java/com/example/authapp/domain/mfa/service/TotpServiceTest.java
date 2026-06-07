package com.example.authapp.domain.mfa.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TotpServiceTest {

    @Test
    void otpAuthUriUsesIssuerPrefixAndUsernameAsVisibleAccountLabel() {
        String uri = new TotpService().otpAuthUri("admin", "ABC123");

        assertThat(uri).startsWith("otpauth://totp/AuthApp:admin?");
        assertThat(uri).contains("secret=ABC123");
        assertThat(uri).contains("issuer=AuthApp");
    }
}
