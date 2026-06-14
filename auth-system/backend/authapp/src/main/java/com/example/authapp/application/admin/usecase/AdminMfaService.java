package com.example.authapp.application.admin.usecase;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.authapp.domain.audit.entity.AdminActionType;
import com.example.authapp.domain.audit.service.AdminActionLogRequest;
import com.example.authapp.domain.audit.service.AdminActionLogService;
import com.example.authapp.domain.mfa.dto.AdminMfaUserResponse;
import com.example.authapp.domain.mfa.dto.MfaExceptionRequest;
import com.example.authapp.domain.mfa.dto.MfaPolicyResponse;
import com.example.authapp.domain.mfa.entity.MfaExceptionEntity;
import com.example.authapp.domain.mfa.entity.MfaMethodEntity;
import com.example.authapp.domain.mfa.entity.MfaPolicy;
import com.example.authapp.domain.mfa.repository.MfaExceptionRepository;
import com.example.authapp.domain.mfa.repository.MfaMethodRepository;
import com.example.authapp.application.auth.usecase.MfaService;
import com.example.authapp.domain.mfa.service.MfaSettingsService;
import com.example.authapp.domain.user.entity.UserEntity;
import com.example.authapp.domain.user.repository.UserRepository;
import com.example.authapp.util.ClientUtil;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminMfaService {

    private final UserRepository userRepository;
    private final MfaMethodRepository mfaMethodRepository;
    private final MfaExceptionRepository mfaExceptionRepository;
    private final MfaService mfaService;
    private final MfaSettingsService mfaSettingsService;
    private final AdminActionLogService adminActionLogService;

    // 전체 사용자의 MFA 상태 목록을 조회한다.
    @Transactional(readOnly = true)
    public List<AdminMfaUserResponse> users() {
        MfaPolicy policy = mfaSettingsService.policy();
        List<UserEntity> users = userRepository.findAll();
        List<String> usernames = users.stream().map(UserEntity::getUsername).toList();

        if (usernames.isEmpty()) {
            return List.of();
        }

        Map<String, MfaMethodEntity> methods = mfaMethodRepository.findByUsernameInAndEnabledTrue(usernames)
                .stream()
                .collect(Collectors.toMap(
                        MfaMethodEntity::getUsername,
                        Function.identity(),
                        this::latestMethod
                ));

        Map<String, MfaExceptionEntity> exceptions = mfaExceptionRepository.findActiveByUsernameIn(usernames)
                .stream()
                .collect(Collectors.toMap(
                        MfaExceptionEntity::getUsername,
                        Function.identity(),
                        (left, right) -> left.getExpiresAt().isAfter(right.getExpiresAt()) ? left : right
                ));

        return users.stream()
                .map(user -> toResponse(user, policy, methods.get(user.getUsername()), exceptions.get(user.getUsername())))
                .toList();
    }

    // 특정 사용자의 MFA 상태를 조회한다.
    @Transactional(readOnly = true)
    public AdminMfaUserResponse user(String username) {
        return toResponse(userRepository.findByUsername(username).orElseThrow(), mfaSettingsService.policy());
    }

    // 특정 사용자의 MFA를 초기화한다.
    public void reset(String username, String reason) {
        reset(username, reason, null);
    }

    // 특정 사용자의 MFA를 초기화하고 관리자 작업 로그를 남긴다.
    public void reset(String username, String reason, HttpServletRequest request) {
        mfaService.resetUserMfa(username);
        saveAction(username, AdminActionType.MFA_RESET, reason, null, "{\"mfaReset\":true}", request);
    }

    // 특정 사용자에게 MFA 예외를 생성한다.
    public MfaExceptionEntity createException(String username, MfaExceptionRequest request) {
        return createException(username, request, null);
    }

    // 특정 사용자에게 MFA 예외를 생성하고 관리자 작업 로그를 남긴다.
    public MfaExceptionEntity createException(String username, MfaExceptionRequest request, HttpServletRequest httpRequest) {
        if (request.expiresAt() == null || !request.expiresAt().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("MFA exception expiry must be in the future.");
        }

        mfaExceptionRepository.findActiveByUsername(username)
                .ifPresent(exception -> exception.revoke(currentActor()));

        MfaExceptionEntity saved = mfaExceptionRepository.save(MfaExceptionEntity.builder()
                .username(username)
                .reason(defaultText(request.reason(), "Temporary MFA exception"))
                .expiresAt(request.expiresAt())
                .createdBy(currentActor())
                .build());

        saveAction(username, AdminActionType.MFA_EXCEPTION_CREATED, request.reason(), null, "{\"expiresAt\":\"" + request.expiresAt() + "\"}", httpRequest);
        return saved;
    }

    // 특정 사용자의 MFA 예외를 해제한다.
    public void revokeException(String username) {
        revokeException(username, null);
    }

    // 특정 사용자의 MFA 예외를 해제하고 관리자 작업 로그를 남긴다.
    public void revokeException(String username, HttpServletRequest request) {
        MfaExceptionEntity exception = mfaExceptionRepository.findActiveByUsername(username).orElseThrow();
        exception.revoke(currentActor());
        saveAction(username, AdminActionType.MFA_EXCEPTION_REVOKED, "Revoke MFA exception", null, "{\"revoked\":true}", request);
    }

    // 현재 MFA 정책을 조회한다.
    @Transactional(readOnly = true)
    public MfaPolicyResponse policy() {
        return new MfaPolicyResponse(mfaSettingsService.policy());
    }

    // MFA 정책을 변경한다.
    public MfaPolicyResponse updatePolicy(MfaPolicy policy) {
        return updatePolicy(policy, null);
    }

    // MFA 정책을 변경하고 관리자 작업 로그를 남긴다.
    public MfaPolicyResponse updatePolicy(MfaPolicy policy, HttpServletRequest request) {
        assertAdminMfaExistsWhenPolicyRequiresAdmins(policy);
        MfaPolicy saved = mfaSettingsService.updatePolicy(policy);
        saveAction("MFA_POLICY", AdminActionType.MFA_POLICY_UPDATED, "Update MFA policy", null, "{\"policy\":\"" + saved + "\"}", request);
        return new MfaPolicyResponse(saved);
    }

    private AdminMfaUserResponse toResponse(UserEntity user, MfaPolicy policy) {
        MfaMethodEntity primary = mfaMethodRepository.findByUsernameAndEnabledTrue(user.getUsername())
                .stream()
                .reduce(this::latestMethod)
                .orElse(null);
        var exception = mfaExceptionRepository.findActiveByUsername(user.getUsername());
        return toResponse(user, policy, primary, exception.orElse(null));
    }

    private AdminMfaUserResponse toResponse(UserEntity user, MfaPolicy policy, MfaMethodEntity primary, MfaExceptionEntity exception) {
        return new AdminMfaUserResponse(
                user.getUsername(),
                user.getEmail(),
                primary != null,
                primary != null ? primary.getType() : null,
                primary != null ? primary.getRegisteredAt() : null,
                primary != null ? primary.getLastUsedAt() : null,
                exception != null,
                exception != null ? exception.getExpiresAt() : null,
                isRequiredByPolicy(user, policy)
        );
    }

    private boolean isRequiredByPolicy(UserEntity user, MfaPolicy policy) {
        MfaPolicy normalized = policy.normalized();
        if (normalized == MfaPolicy.REQUIRED_FOR_ALL) {
            return true;
        }
        return normalized == MfaPolicy.REQUIRED_FOR_ADMIN
                && user.getRoles().stream().anyMatch(role -> "ROLE_ADMIN".equals(role.getName()));
    }

    private void assertAdminMfaExistsWhenPolicyRequiresAdmins(MfaPolicy policy) {
        MfaPolicy normalized = policy == null ? MfaPolicy.OPTIONAL : policy.normalized();
        if (normalized != MfaPolicy.REQUIRED_FOR_ADMIN && normalized != MfaPolicy.REQUIRED_FOR_ALL) {
            return;
        }

        boolean hasProtectedAdmin = userRepository.findAll()
                .stream()
                .filter(this::isAdmin)
                .anyMatch(user -> mfaMethodRepository.existsByUsernameAndEnabledTrue(user.getUsername()));

        if (!hasProtectedAdmin) {
            throw new IllegalStateException("At least one admin account must have MFA registered before enabling admin MFA policy.");
        }
    }

    private boolean isAdmin(UserEntity user) {
        return user.getRoles().stream().anyMatch(role -> "ROLE_ADMIN".equals(role.getName()));
    }

    private MfaMethodEntity latestMethod(MfaMethodEntity left, MfaMethodEntity right) {
        LocalDateTime leftRegistered = left.getRegisteredAt();
        LocalDateTime rightRegistered = right.getRegisteredAt();
        if (leftRegistered == null) {
            return rightRegistered == null ? left : right;
        }
        if (rightRegistered == null) {
            return left;
        }
        return leftRegistered.isAfter(rightRegistered) ? left : right;
    }

    private void saveAction(String targetUsername, AdminActionType actionType, String reason, String beforeValue, String afterValue, HttpServletRequest request) {
        String userAgent = request != null ? ClientUtil.getUserAgent(request) : null;
        adminActionLogService.record(AdminActionLogRequest.builder()
                .actorUsername(currentActor())
                .targetType("USER")
                .targetId(targetUsername)
                .targetUsername(targetUsername)
                .targetName(targetUsername)
                .actionType(actionType)
                .reason(defaultText(reason, actionType.name()))
                .beforeValue(beforeValue)
                .afterValue(afterValue)
                .ipAddress(request != null ? ClientUtil.getIp(request) : null)
                .device(userAgent != null ? ClientUtil.getDevice(userAgent) : null)
                .userAgent(userAgent)
                .build());
    }

    private String currentActor() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            return "UNKNOWN";
        }
        return authentication.getName();
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}

