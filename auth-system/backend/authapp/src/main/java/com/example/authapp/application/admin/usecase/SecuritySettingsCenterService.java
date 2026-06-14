package com.example.authapp.application.admin.usecase;

import java.util.Comparator;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.authapp.domain.admin.security.entity.AdminSecuritySettingsEntity;
import com.example.authapp.domain.admin.security.repository.AdminSecuritySettingsRepository;
import com.example.authapp.domain.admin.settings.dto.SecuritySettingsCenterResponse;
import com.example.authapp.domain.admin.settings.dto.SecuritySettingsUpdateRequest;
import com.example.authapp.domain.admin.settings.service.SecuritySettingsValidator;
import com.example.authapp.domain.audit.entity.AuditSettingsEntity;
import com.example.authapp.domain.audit.repository.AuditSettingsRepository;
import com.example.authapp.domain.auth.lockout.entity.AccountLockoutSettingsEntity;
import com.example.authapp.domain.auth.lockout.repository.AccountLockoutSettingsRepository;
import com.example.authapp.domain.auth.login.entity.LoginSettingsEntity;
import com.example.authapp.domain.auth.login.repository.LoginSettingsRepository;
import com.example.authapp.domain.auth.password.entity.PasswordSettingsEntity;
import com.example.authapp.domain.auth.password.repository.PasswordSettingsRepository;
import com.example.authapp.domain.auth.verification.entity.VerificationTokenSettingsEntity;
import com.example.authapp.domain.auth.verification.repository.VerificationTokenSettingsRepository;
import com.example.authapp.domain.jwt.entity.TokenSettingsEntity;
import com.example.authapp.domain.jwt.repository.TokenSettingsRepository;
import com.example.authapp.domain.mfa.entity.MfaSettingsEntity;
import com.example.authapp.domain.mfa.repository.MfaSettingsRepository;
import com.example.authapp.domain.risk.entity.RiskDetectionSettingsEntity;
import com.example.authapp.domain.risk.repository.RiskDetectionSettingsRepository;
import com.example.authapp.domain.risk.repository.RiskRuleSettingsRepository;
import com.example.authapp.domain.session.entity.SessionSettingsEntity;
import com.example.authapp.domain.session.repository.SessionSettingsRepository;
import com.example.authapp.domain.system.settings.entity.RateLimitSettingsEntity;
import com.example.authapp.domain.system.settings.entity.WebSecuritySettingsEntity;
import com.example.authapp.domain.system.settings.repository.RateLimitSettingsRepository;
import com.example.authapp.domain.system.settings.repository.WebSecuritySettingsRepository;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SecuritySettingsCenterService {

    private final LoginSettingsRepository loginRepository;
    private final PasswordSettingsRepository passwordRepository;
    private final AccountLockoutSettingsRepository lockoutRepository;
    private final MfaSettingsRepository mfaRepository;
    private final VerificationTokenSettingsRepository verificationTokenRepository;
    private final TokenSettingsRepository tokenRepository;
    private final SessionSettingsRepository sessionRepository;
    private final RiskDetectionSettingsRepository riskDetectionRepository;
    private final RiskRuleSettingsRepository riskRuleRepository;
    private final AdminSecuritySettingsRepository adminSecurityRepository;
    private final WebSecuritySettingsRepository webSecurityRepository;
    private final AuditSettingsRepository auditRepository;
    private final RateLimitSettingsRepository rateLimitRepository;
    private final SecuritySettingsAuditService auditService;

    @Transactional(readOnly = true)
    public SecuritySettingsCenterResponse currentSettings() {
        return response();
    }

    @Transactional
    public SecuritySettingsCenterResponse update(
            SecuritySettingsUpdateRequest request,
            String actorUsername,
            HttpServletRequest httpRequest
    ) {
        SecuritySettingsValidator.validate(request);
        SecuritySettingsCenterResponse before = currentSettings();

        loginRepository.save(request.authentication().login().toEntity());
        passwordRepository.save(request.authentication().password().toEntity());
        lockoutRepository.save(request.authentication().lockout().toEntity());
        mfaRepository.save(request.authentication().mfa().toEntity());
        verificationTokenRepository.save(request.authentication().verificationToken().toEntity());
        tokenRepository.save(request.sessionToken().toTokenEntity());
        sessionRepository.save(request.sessionToken().toSessionEntity());
        riskDetectionRepository.save(request.riskDetection().toEntity());
        adminSecurityRepository.save(request.operationalSecurity().adminAccess().toEntity());
        webSecurityRepository.save(request.operationalSecurity().corsRedirect().toEntity());
        auditRepository.save(request.operationalSecurity().logRetention().toEntity());
        rateLimitRepository.save(request.operationalSecurity().rateLimit().toEntity());
        if (request.riskRules() != null) {
            riskRuleRepository.saveAll(request.riskRules().stream()
                    .map(SecuritySettingsCenterResponse.RiskRuleSettings::toEntity)
                    .toList());
        }

        SecuritySettingsCenterResponse after = currentSettings();
        auditService.recordUpdate(actorUsername, before, after, httpRequest);
        return after;
    }

    private SecuritySettingsCenterResponse response() {
        return new SecuritySettingsCenterResponse(
                SecuritySettingsCenterResponse.AuthenticationSettings.from(
                        login(),
                        password(),
                        lockout(),
                        mfa(),
                        verificationToken()
                ),
                SecuritySettingsCenterResponse.SessionTokenSettings.from(token(), session()),
                SecuritySettingsCenterResponse.RiskDetectionSettings.from(riskDetection()),
                riskRuleRepository.findAll()
                        .stream()
                        .sorted(Comparator.comparing(rule -> rule.getEventType().name()))
                        .map(SecuritySettingsCenterResponse.RiskRuleSettings::from)
                        .toList(),
                SecuritySettingsCenterResponse.OperationalSecuritySettings.from(
                        adminSecurity(),
                        webSecurity(),
                        audit(),
                        rateLimit()
                )
        );
    }

    private LoginSettingsEntity login() {
        return loginRepository.findById(LoginSettingsEntity.SETTINGS_ID).orElseGet(LoginSettingsEntity::defaults);
    }

    private PasswordSettingsEntity password() {
        return passwordRepository.findById(PasswordSettingsEntity.SETTINGS_ID).orElseGet(PasswordSettingsEntity::defaults);
    }

    private AccountLockoutSettingsEntity lockout() {
        return lockoutRepository.findById(AccountLockoutSettingsEntity.SETTINGS_ID).orElseGet(AccountLockoutSettingsEntity::defaults);
    }

    private MfaSettingsEntity mfa() {
        return mfaRepository.findById(MfaSettingsEntity.SETTINGS_ID).orElseGet(MfaSettingsEntity::defaults);
    }

    private VerificationTokenSettingsEntity verificationToken() {
        return verificationTokenRepository.findById(VerificationTokenSettingsEntity.SETTINGS_ID)
                .orElseGet(VerificationTokenSettingsEntity::defaults);
    }

    private TokenSettingsEntity token() {
        return tokenRepository.findById(TokenSettingsEntity.SETTINGS_ID).orElseGet(TokenSettingsEntity::defaults);
    }

    private SessionSettingsEntity session() {
        return sessionRepository.findById(SessionSettingsEntity.SETTINGS_ID).orElseGet(SessionSettingsEntity::defaults);
    }

    private RiskDetectionSettingsEntity riskDetection() {
        return riskDetectionRepository.findById(RiskDetectionSettingsEntity.SETTINGS_ID)
                .orElseGet(RiskDetectionSettingsEntity::defaults);
    }

    private AdminSecuritySettingsEntity adminSecurity() {
        return adminSecurityRepository.findById(AdminSecuritySettingsEntity.SETTINGS_ID)
                .orElseGet(AdminSecuritySettingsEntity::defaults);
    }

    private WebSecuritySettingsEntity webSecurity() {
        return webSecurityRepository.findById(WebSecuritySettingsEntity.SETTINGS_ID)
                .orElseGet(WebSecuritySettingsEntity::defaults);
    }

    private AuditSettingsEntity audit() {
        return auditRepository.findById(AuditSettingsEntity.SETTINGS_ID).orElseGet(AuditSettingsEntity::defaults);
    }

    private RateLimitSettingsEntity rateLimit() {
        return rateLimitRepository.findById(RateLimitSettingsEntity.SETTINGS_ID)
                .orElseGet(RateLimitSettingsEntity::defaults);
    }
}
