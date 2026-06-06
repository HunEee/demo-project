package com.example.authapp.domain.admin;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.example.authapp.domain.admin.dto.AdminActionLogResponse;
import com.example.authapp.domain.admin.dto.AdminAuditLogResponse;
import com.example.authapp.domain.admin.dto.AdminDashboardSummaryResponse;
import com.example.authapp.domain.admin.dto.AdminFilterOption;
import com.example.authapp.domain.admin.dto.AdminFilterOptionsResponse;
import com.example.authapp.domain.admin.dto.AdminIncidentResponse;
import com.example.authapp.domain.admin.dto.AdminLoginHistoryResponse;
import com.example.authapp.domain.admin.dto.AdminPasswordResetResponse;
import com.example.authapp.domain.admin.dto.AdminRiskResponse;
import com.example.authapp.domain.admin.dto.AdminSessionResponse;
import com.example.authapp.domain.admin.dto.AdminUserCreateRequest;
import com.example.authapp.domain.admin.dto.AdminUserDetailResponse;
import com.example.authapp.domain.admin.dto.AdminUserResponse;
import com.example.authapp.domain.admin.dto.AdminUserUpdateRequest;
import com.example.authapp.domain.admin.type.AdminSessionStatus;
import com.example.authapp.domain.admin.type.AdminUserStatus;
import com.example.authapp.domain.audit.entity.AdminActionLogEntity;
import com.example.authapp.domain.audit.entity.AdminActionType;
import com.example.authapp.domain.audit.entity.AuthEventLogEntity;
import com.example.authapp.domain.audit.entity.AuthEventType;
import com.example.authapp.domain.audit.entity.LoginHistoryEntity;
import com.example.authapp.domain.audit.entity.LoginStatus;
import com.example.authapp.domain.audit.entity.SecurityIncidentEntity;
import com.example.authapp.domain.audit.entity.SecurityIncidentType;
import com.example.authapp.domain.audit.entity.Severity;
import com.example.authapp.domain.audit.repository.AdminActionLogRepository;
import com.example.authapp.domain.audit.repository.AuthEventLogRepository;
import com.example.authapp.domain.audit.repository.LoginHistoryRepository;
import com.example.authapp.domain.audit.repository.SecurityIncidentRepository;
import com.example.authapp.domain.authorization.entity.RoleEntity;
import com.example.authapp.domain.authorization.repository.RoleRepository;
import com.example.authapp.domain.hr.entity.HrUserMasterEntity;
import com.example.authapp.domain.hr.repository.HrUserMasterRepository;
import com.example.authapp.domain.jwt.entity.RefreshTokenEntity;
import com.example.authapp.domain.jwt.repository.RefreshTokenRepository;
import com.example.authapp.domain.mfa.repository.MfaMethodRepository;
import com.example.authapp.domain.organization.dto.AdminGroupResponse;
import com.example.authapp.domain.organization.repository.GroupUserRepository;
import com.example.authapp.domain.risk.entity.RiskEntity;
import com.example.authapp.domain.risk.entity.RiskLevel;
import com.example.authapp.domain.risk.repository.RiskRepository;
import com.example.authapp.domain.user.entity.UserEntity;
import com.example.authapp.domain.user.entity.UserRoleType;
import com.example.authapp.domain.user.repository.UserRepository;
import com.example.authapp.util.ClientUtil;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminConsoleService {

    private final UserRepository userRepository;
    private final LoginHistoryRepository loginHistoryRepository;
    private final AuthEventLogRepository authEventLogRepository;
    private final SecurityIncidentRepository securityIncidentRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RiskRepository riskRepository;
    private final HrUserMasterRepository hrUserMasterRepository;
    private final MfaMethodRepository mfaMethodRepository;
    private final GroupUserRepository groupUserRepository;
    private final RoleRepository roleRepository;
    private final AdminActionLogRepository adminActionLogRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public AdminDashboardSummaryResponse dashboardSummary() {
        List<UserEntity> users = userRepository.findAll();
        List<RefreshTokenEntity> sessions = refreshTokenRepository.findAll();
        List<SecurityIncidentEntity> incidents = securityIncidentRepository.findAll();
        List<RiskEntity> risks = riskRepository.findAll();

        return new AdminDashboardSummaryResponse(
                users.size(),
                users.stream().filter(user -> user.isEnabled() && !user.isLocked()).count(),
                users.stream().filter(UserEntity::isLocked).count(),
                users.stream().filter(user -> user.getRoles().stream().anyMatch(role -> "ROLE_ADMIN".equals(role.getName()))).count(),
                sessions.size(),
                sessions.stream().filter(RefreshTokenEntity::isRevoked).count(),
                incidents.stream().filter(incident -> !incident.isResolved()).count(),
                risks.stream().filter(risk -> risk.getRiskScore() >= 60).count()
        );
    }

    @Transactional(readOnly = true)
    public AdminFilterOptionsResponse filterOptions() {
        return new AdminFilterOptionsResponse(
                enumOptions(AdminUserStatus.values(), AdminUserStatus::getLabel),
                enumOptions(UserRoleType.values(), UserRoleType::getLabel),
                enumOptions(AuthEventType.values(), AuthEventType::getLabel),
                enumOptions(LoginStatus.values(), LoginStatus::getLabel),
                enumOptions(SecurityIncidentType.values(), SecurityIncidentType::getLabel),
                enumOptions(Severity.values(), Severity::getLabel),
                enumOptions(AdminSessionStatus.values(), AdminSessionStatus::getLabel),
                enumOptions(RiskLevel.values(), RiskLevel::getLabel)
        );
    }

    @Transactional(readOnly = true)
    public Page<AdminUserResponse> users(int page, int size, String keyword, String status, String role, String sort, String direction) {
        return users(page, size, keyword, status, role, sort, direction, null, null, null, null);
    }

    @Transactional(readOnly = true)
    public Page<AdminUserResponse> users(
            int page,
            int size,
            String keyword,
            String status,
            String role,
            String sort,
            String direction,
            String departmentCode,
            Boolean directOnly,
            String authMethod,
            Boolean mfaEnabled
    ) {
        List<AdminUserResponse> content = userRepository.findAll().stream()
                .filter(user -> {
                    HrUserMasterEntity hrUser = findHrUser(user.getUsername());
                    boolean userMfaEnabled = mfaMethodRepository.existsByUsernameAndEnabledTrue(user.getUsername());
                    return keywordMatches(user, hrUser, keyword)
                            && (status == null || status.isBlank() || statusMatches(user, hrUser, status))
                            && departmentMatches(hrUser, departmentCode)
                            && directRegistrationMatches(hrUser, directOnly)
                            && authMethodMatches(user, authMethod)
                            && mfaMatches(userMfaEnabled, mfaEnabled);
                })
                .filter(user -> role == null || role.isBlank() || user.getRoles().stream().anyMatch(item -> item.getName().equals(role)))
                .sorted(applyDirection(userComparator(sort), direction))
                .map(this::toAdminUserResponse)
                .toList();

        return page(content, page, size);
    }

    @Transactional
    public AdminUserResponse createUser(AdminUserCreateRequest request) {
        requireText(request.employeeNo(), "Employee number is required.");
        requireText(request.username(), "Username is required.");
        requireText(request.password(), "Password is required.");
        HrUserMasterEntity hrUser = hrUserMasterRepository.findByEmployeeNo(request.employeeNo()).orElseThrow();
        if (!hrUser.canCreateAccount()) {
            throw new IllegalStateException("HR user is not available for account creation.");
        }
        if (userRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Username already exists.");
        }
        if (userRepository.existsByEmail(hrUser.getEmail())) {
            throw new IllegalArgumentException("Email already exists.");
        }

        RoleEntity role = roleRepository.findByName(defaultText(request.roleName(), "ROLE_USER")).orElseThrow();
        UserEntity user = UserEntity.builder()
                .username(request.username())
                .password(passwordEncoder.encode(request.password()))
                .email(hrUser.getEmail())
                .nickname(hrUser.getName())
                .locked(false)
                .enabled(true)
                .social(false)
                .build();
        user.addRole(role);
        UserEntity saved = userRepository.save(user);
        hrUser.markAccountCreated(saved.getUsername());
        saveAdminAction(saved.getUsername(), AdminActionType.PROFILE_UPDATE, null, accountState(saved) + hrState(hrUser), defaultText(request.reason(), "Create account from HR master"));
        return AdminUserResponse.from(saved, hrUser, false, null);
    }

    @Transactional(readOnly = true)
    public boolean usernameExists(String username) {
        return username != null && !username.isBlank() && userRepository.existsByUsername(username.trim());
    }

    @Transactional
    public AdminUserResponse updateUser(String username, AdminUserUpdateRequest request) {
        UserEntity user = userRepository.findByUsername(username).orElseThrow();
        HrUserMasterEntity hrUser = findHrUser(username);
        String before = accountState(user) + hrState(hrUser);
        user.updateOAuthProfile(request.email(), request.name());
        if (request.locked() != null) {
            if (request.locked()) {
                user.lock();
            } else {
                user.unlock();
            }
        }
        if (request.enabled() != null) {
            if (request.enabled()) {
                user.enable();
            } else {
                user.disable();
                Optional.ofNullable(hrUser).ifPresent(HrUserMasterEntity::markAccountDisabled);
            }
        }
        boolean mfaEnabled = mfaMethodRepository.existsByUsernameAndEnabledTrue(username);
        saveAdminAction(username, AdminActionType.PROFILE_UPDATE, before, accountState(user) + hrState(hrUser), defaultText(request.reason(), "Update user account"));
        return AdminUserResponse.from(user, hrUser, mfaEnabled, latestSuccessfulLoginAt(username));
    }

    @Transactional
    public void deleteUser(String username, String reason) {
        UserEntity user = userRepository.findByUsername(username).orElseThrow();
        String before = accountState(user);
        user.deactivate();
        Optional.ofNullable(findHrUser(username)).ifPresent(HrUserMasterEntity::markAccountDisabled);
        saveAdminAction(username, AdminActionType.PROFILE_UPDATE, before, accountState(user), defaultText(reason, "Delete user account"));
    }

    @Transactional(readOnly = true)
    public AdminUserDetailResponse userDetail(String username) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        List<AdminLoginHistoryResponse> recentLogins = loginHistoryRepository
                .findTop20ByUsernameOrderByLoginAtDesc(username)
                .stream()
                .map(AdminLoginHistoryResponse::from)
                .toList();

        List<AdminAuditLogResponse> recentEvents = authEventLogRepository
                .findByUsernameOrderByCreatedAtDesc(username)
                .stream()
                .limit(20)
                .map(AdminAuditLogResponse::from)
                .toList();

        List<AdminSessionResponse> sessions = refreshTokenRepository
                .findByUsername(username)
                .stream()
                .map(AdminSessionResponse::from)
                .toList();

        List<AdminActionLogResponse> adminActions = adminActionLogRepository
                .findByTargetUsernameOrderByCreatedAtDesc(username)
                .stream()
                .limit(20)
                .map(AdminActionLogResponse::from)
                .toList();

        AdminRiskResponse risk = riskRepository.findByUserUsername(username)
                .map(AdminRiskResponse::from)
                .orElse(null);

        List<AdminGroupResponse> groups = groupUserRepository.findByUsername(username)
                .stream()
                .map(groupUser -> AdminGroupResponse.from(
                        groupUser.getGroup(),
                        groupUserRepository.countByGroupId(groupUser.getGroup().getId()),
                        0
                ))
                .toList();

        return new AdminUserDetailResponse(toAdminUserResponse(user), recentLogins, recentEvents, sessions, adminActions, risk, groups);
    }

    @Transactional
    public void lockUser(String username) {
        UserEntity user = userRepository.findByUsername(username).orElseThrow();
        String before = accountState(user);
        user.lock();
        saveAdminAction(username, AdminActionType.LOCK_USER, before, accountState(user), "Lock user account");
    }

    @Transactional
    public void unlockUser(String username) {
        UserEntity user = userRepository.findByUsername(username).orElseThrow();
        String before = accountState(user);
        user.unlock();
        saveAdminAction(username, AdminActionType.UNLOCK_USER, before, accountState(user), "Unlock user account");
    }

    @Transactional
    public void disableUser(String username) {
        UserEntity user = userRepository.findByUsername(username).orElseThrow();
        String before = accountState(user);
        user.disable();
        Optional.ofNullable(findHrUser(username)).ifPresent(HrUserMasterEntity::markAccountDisabled);
        saveAdminAction(username, AdminActionType.DISABLE_USER, before, accountState(user), "Disable user account");
    }

    @Transactional
    public void enableUser(String username) {
        UserEntity user = userRepository.findByUsername(username).orElseThrow();
        String before = accountState(user);
        user.enable();
        saveAdminAction(username, AdminActionType.ENABLE_USER, before, accountState(user), "Enable user account");
    }

    @Transactional
    public void revokeUserTokens(String username) {
        refreshTokenRepository.findByUsername(username).forEach(RefreshTokenEntity::revoke);
        saveAdminAction(username, AdminActionType.TOKEN_REVOKE, null, "{\"revoked\":true}", "Revoke all user tokens");
    }

    @Transactional
    public AdminPasswordResetResponse resetPassword(String username) {
        UserEntity user = userRepository.findByUsername(username).orElseThrow();
        String temporaryPassword = "Temp-" + UUID.randomUUID().toString().substring(0, 8);
        user.changePassword(passwordEncoder.encode(temporaryPassword));
        saveAdminAction(username, AdminActionType.PASSWORD_RESET, null, "{\"passwordReset\":true}", "Reset user password");
        return new AdminPasswordResetResponse(username, temporaryPassword);
    }

    @Transactional
    public void resetMfa(String username) {
        mfaMethodRepository.deleteByUsername(username);
        saveAdminAction(username, AdminActionType.MFA_RESET, null, "{\"mfaReset\":true}", "Reset user MFA");
    }

    @Transactional(readOnly = true)
    public Page<AdminAuditLogResponse> auditLogs(int page, int size, String username, String type, String from, String to, String sort, String direction) {
        List<AdminAuditLogResponse> content = authEventLogRepository.findAll().stream()
                .filter(event -> contains(event.getUsername(), username))
                .filter(event -> type == null || type.isBlank() || event.getType().name().equalsIgnoreCase(type))
                .filter(event -> matchesDate(event.getCreatedAt(), from, to))
                .sorted(applyDirection(auditComparator(sort), direction))
                .map(AdminAuditLogResponse::from)
                .toList();

        return page(content, page, size);
    }

    @Transactional(readOnly = true)
    public Page<AdminLoginHistoryResponse> loginHistory(int page, int size, String username, String status, String from, String to, String sort, String direction) {
        List<AdminLoginHistoryResponse> content = loginHistoryRepository.findAll().stream()
                .filter(history -> contains(history.getUsername(), username))
                .filter(history -> status == null || status.isBlank() || history.getStatus().name().equalsIgnoreCase(status))
                .filter(history -> matchesDate(history.getLoginAt(), from, to))
                .sorted(applyDirection(loginComparator(sort), direction))
                .map(AdminLoginHistoryResponse::from)
                .toList();

        return page(content, page, size);
    }

    @Transactional(readOnly = true)
    public Page<AdminAuditLogResponse> securityEvents(int page, int size, String username, String type, String from, String to, String sort, String direction) {
        return auditLogs(page, size, username, type, from, to, sort, direction);
    }

    @Transactional(readOnly = true)
    public Page<AdminIncidentResponse> incidents(int page, int size, String username, String type, String severity, Boolean resolved, String from, String to, String sort, String direction) {
        List<AdminIncidentResponse> content = securityIncidentRepository.findAll().stream()
                .filter(incident -> contains(incident.getUsername(), username))
                .filter(incident -> type == null || type.isBlank() || incident.getType().name().equalsIgnoreCase(type))
                .filter(incident -> severity == null || severity.isBlank() || incident.getSeverity().name().equalsIgnoreCase(severity))
                .filter(incident -> resolved == null || incident.isResolved() == resolved)
                .filter(incident -> matchesDate(incident.getCreatedAt(), from, to))
                .sorted(applyDirection(incidentComparator(sort), direction))
                .map(AdminIncidentResponse::from)
                .toList();

        return page(content, page, size);
    }

    @Transactional
    public void resolveIncident(Long id, String adminUsername) {
        securityIncidentRepository.findById(id).orElseThrow().resolve(adminUsername);
    }

    @Transactional(readOnly = true)
    public Page<AdminSessionResponse> sessions(int page, int size, String username, Boolean activeOnly, String status, String device, String from, String to, String sort, String direction) {
        List<AdminSessionResponse> content = refreshTokenRepository.findAll().stream()
                .filter(token -> contains(token.getUsername(), username))
                .filter(token -> contains(token.getDevice(), device))
                .filter(token -> activeOnly == null || !activeOnly || !token.isRevoked())
                .filter(token -> sessionStatusMatches(token, status))
                .filter(token -> matchesDate(Optional.ofNullable(token.getLastUsedAt()).orElse(token.getCreatedAt()), from, to))
                .sorted(applyDirection(sessionComparator(sort), direction))
                .map(AdminSessionResponse::from)
                .toList();

        return page(content, page, size);
    }

    @Transactional
    public void revokeSession(Long id) {
        refreshTokenRepository.findById(id).orElseThrow().revoke();
    }

    @Transactional(readOnly = true)
    public Page<AdminRiskResponse> risks(int page, int size, String username, String level, Integer minScore, String sort, String direction) {
        List<AdminRiskResponse> content = riskRepository.findAll().stream()
                .filter(risk -> contains(risk.getUsername(), username))
                .filter(risk -> level == null || level.isBlank() || Optional.ofNullable(risk.getRiskLevel()).map(Enum::name).orElse("LOW").equalsIgnoreCase(level))
                .filter(risk -> minScore == null || risk.getRiskScore() >= minScore)
                .sorted(applyDirection(riskComparator(sort), direction))
                .map(AdminRiskResponse::from)
                .toList();

        return page(content, page, size);
    }

    private boolean contains(String value, String keyword) {
        if (keyword == null || keyword.isBlank()) return true;
        if (value == null) return false;
        return value.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT));
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(message);
    }

    private boolean keywordMatches(UserEntity user, HrUserMasterEntity hrUser, String keyword) {
        return contains(user.getUsername(), keyword)
                || contains(user.getEmail(), keyword)
                || contains(user.getNickname(), keyword)
                || (hrUser != null && contains(hrUser.getEmployeeNo(), keyword))
                || (hrUser != null && contains(hrUser.getName(), keyword))
                || (hrUser != null && contains(hrUser.getDepartmentName(), keyword))
                || (hrUser != null && contains(hrUser.getPosition(), keyword));
    }

    private boolean statusMatches(UserEntity user, HrUserMasterEntity hrUser, String status) {
        return switch (status.toUpperCase(Locale.ROOT)) {
            case "DELETED" -> user.isDeleted();
            case "LOCKED" -> !user.isDeleted() && user.isLocked();
            case "ACTIVE" -> !user.isDeleted() && user.isEnabled() && !user.isLocked()
                    && (hrUser == null || hrUser.getHrStatus() == null || "ACTIVE".equals(hrUser.getHrStatus().name()));
            case "DISABLED" -> !user.isDeleted() && !user.isEnabled();
            case "LEAVE", "RETIRED", "SUSPENDED" -> hrUser != null
                    && hrUser.getHrStatus() != null
                    && status.equalsIgnoreCase(hrUser.getHrStatus().name());
            default -> true;
        };
    }

    private boolean departmentMatches(HrUserMasterEntity hrUser, String departmentCode) {
        if (departmentCode == null || departmentCode.isBlank()) return true;
        return hrUser != null && contains(hrUser.getDepartmentCode(), departmentCode);
    }

    private boolean directRegistrationMatches(HrUserMasterEntity hrUser, Boolean directOnly) {
        return directOnly == null || !directOnly || hrUser == null;
    }

    private boolean authMethodMatches(UserEntity user, String authMethod) {
        if (authMethod == null || authMethod.isBlank()) return true;
        String normalized = authMethod.toUpperCase(Locale.ROOT);
        if ("PASSWORD".equals(normalized)) return !user.isSocial();
        if ("SOCIAL".equals(normalized)) return user.isSocial();
        return user.isSocial()
                && user.getSocialProviderType() != null
                && user.getSocialProviderType().name().equalsIgnoreCase(authMethod);
    }

    private boolean mfaMatches(boolean userMfaEnabled, Boolean mfaEnabled) {
        return mfaEnabled == null || userMfaEnabled == mfaEnabled;
    }

    private boolean sessionStatusMatches(RefreshTokenEntity token, String status) {
        if (status == null || status.isBlank()) return true;
        return switch (status.toUpperCase(Locale.ROOT)) {
            case "ACTIVE" -> !token.isRevoked();
            case "REVOKED" -> token.isRevoked();
            default -> true;
        };
    }

    private Comparator<UserEntity> userComparator(String sort) {
        return switch (normalizeSort(sort)) {
            case "username" -> compareText(UserEntity::getUsername);
            case "email" -> compareText(UserEntity::getEmail);
            case "enabled" -> Comparator.comparing(UserEntity::isEnabled);
            case "locked" -> Comparator.comparing(UserEntity::isLocked);
            case "createdAt" -> compareValue(UserEntity::getCreatedAt);
            default -> Comparator.comparing(UserEntity::getId);
        };
    }

    private Comparator<AuthEventLogEntity> auditComparator(String sort) {
        return switch (normalizeSort(sort)) {
            case "username" -> compareText(AuthEventLogEntity::getUsername);
            case "type" -> compareText(event -> event.getType().name());
            case "ipAddress" -> compareText(AuthEventLogEntity::getIpAddress);
            default -> compareValue(AuthEventLogEntity::getCreatedAt);
        };
    }

    private Comparator<LoginHistoryEntity> loginComparator(String sort) {
        return switch (normalizeSort(sort)) {
            case "username" -> compareText(LoginHistoryEntity::getUsername);
            case "status" -> compareText(history -> history.getStatus().name());
            case "ipAddress" -> compareText(LoginHistoryEntity::getIpAddress);
            case "device" -> compareText(LoginHistoryEntity::getDevice);
            default -> compareValue(LoginHistoryEntity::getLoginAt);
        };
    }

    private Comparator<SecurityIncidentEntity> incidentComparator(String sort) {
        return switch (normalizeSort(sort)) {
            case "username" -> compareText(SecurityIncidentEntity::getUsername);
            case "type" -> compareText(incident -> incident.getType().name());
            case "severity" -> compareText(incident -> incident.getSeverity().name());
            case "resolved" -> Comparator.comparing(SecurityIncidentEntity::isResolved);
            default -> compareValue(SecurityIncidentEntity::getCreatedAt);
        };
    }

    private Comparator<RefreshTokenEntity> sessionComparator(String sort) {
        return switch (normalizeSort(sort)) {
            case "username" -> compareText(RefreshTokenEntity::getUsername);
            case "device" -> compareText(RefreshTokenEntity::getDevice);
            case "ipAddress" -> compareText(RefreshTokenEntity::getIpAddress);
            case "expiresAt" -> compareValue(RefreshTokenEntity::getExpiresAt);
            case "revoked" -> Comparator.comparing(RefreshTokenEntity::isRevoked);
            default -> compareValue(token -> Optional.ofNullable(token.getLastUsedAt()).orElse(token.getCreatedAt()));
        };
    }

    private Comparator<RiskEntity> riskComparator(String sort) {
        return switch (normalizeSort(sort)) {
            case "username" -> compareText(RiskEntity::getUsername);
            case "riskLevel" -> compareText(risk -> Optional.ofNullable(risk.getRiskLevel()).map(Enum::name).orElse("LOW"));
            case "lastReason" -> compareText(RiskEntity::getLastReason);
            case "updatedAt" -> compareValue(RiskEntity::getUpdatedAt);
            default -> Comparator.comparing(RiskEntity::getRiskScore);
        };
    }

    private <T> Comparator<T> applyDirection(Comparator<T> comparator, String direction) {
        return "ASC".equalsIgnoreCase(direction) ? comparator : comparator.reversed();
    }

    private String normalizeSort(String sort) {
        return sort == null ? "" : sort;
    }

    private <T> Comparator<T> compareText(Function<T, String> getter) {
        return Comparator.comparing(getter, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
    }

    private <T, U extends Comparable<? super U>> Comparator<T> compareValue(Function<T, U> getter) {
        return Comparator.comparing(getter, Comparator.nullsLast(Comparator.naturalOrder()));
    }

    private boolean matchesDate(LocalDateTime value, String from, String to) {
        if (value == null) return from == null && to == null;

        LocalDateTime fromDate = parseStartOfDay(from);
        LocalDateTime toDate = parseEndOfDay(to);

        if (fromDate != null && value.isBefore(fromDate)) return false;
        if (toDate != null && value.isAfter(toDate)) return false;
        return true;
    }

    private LocalDateTime parseStartOfDay(String date) {
        if (date == null || date.isBlank()) return null;
        return LocalDate.parse(date).atStartOfDay();
    }

    private LocalDateTime parseEndOfDay(String date) {
        if (date == null || date.isBlank()) return null;
        return LocalDate.parse(date).plusDays(1).atStartOfDay().minusNanos(1);
    }

    private <T> Page<T> page(List<T> content, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);
        int from = Math.min(safePage * safeSize, content.size());
        int to = Math.min(from + safeSize, content.size());
        Pageable pageable = PageRequest.of(safePage, safeSize);
        return new PageImpl<>(content.subList(from, to), pageable, content.size());
    }

    private AdminUserResponse toAdminUserResponse(UserEntity user) {
        String username = user.getUsername();
        boolean mfaEnabled = mfaMethodRepository.existsByUsernameAndEnabledTrue(username);
        return AdminUserResponse.from(user, findHrUser(username), mfaEnabled, latestSuccessfulLoginAt(username));
    }

    private LocalDateTime latestSuccessfulLoginAt(String username) {
        LoginHistoryEntity latestLogin = loginHistoryRepository.findTopByUsernameAndSuccessTrueOrderByLoginAtDesc(username);
        return latestLogin != null ? latestLogin.getLoginAt() : null;
    }

    private HrUserMasterEntity findHrUser(String username) {
        return hrUserMasterRepository.findByAccountUsername(username).orElse(null);
    }

    private void saveAdminAction(
            String targetUsername,
            AdminActionType actionType,
            String beforeValue,
            String afterValue,
            String reason
    ) {
        HttpServletRequest request = currentRequest();
        String userAgent = request != null ? ClientUtil.getUserAgent(request) : null;
        adminActionLogRepository.save(AdminActionLogEntity.builder()
                .actorUsername(currentActor())
                .targetType("USER")
                .targetId(targetUsername)
                .targetUsername(targetUsername)
                .targetName(targetUsername)
                .actionType(actionType)
                .reason(reason)
                .beforeValue(beforeValue)
                .afterValue(afterValue)
                .ipAddress(request != null ? ClientUtil.getIp(request) : "UNKNOWN")
                .device(userAgent != null ? ClientUtil.getDevice(userAgent) : "UNKNOWN")
                .build());
    }

    private String currentActor() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) return "UNKNOWN";
        return authentication.getName();
    }

    private HttpServletRequest currentRequest() {
        var attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletRequestAttributes) {
            return servletRequestAttributes.getRequest();
        }
        return null;
    }

    private String accountState(UserEntity user) {
        return "{\"locked\":" + user.isLocked()
                + ",\"enabled\":" + user.isEnabled()
                + ",\"deleted\":" + user.isDeleted()
                + "}";
    }

    private String hrState(HrUserMasterEntity hrUser) {
        if (hrUser == null) return "{}";
        return "{\"employeeNo\":\"" + Optional.ofNullable(hrUser.getEmployeeNo()).orElse("")
                + "\",\"employmentType\":\"" + Optional.ofNullable(hrUser.getEmploymentType()).map(Enum::name).orElse("")
                + "\",\"hrStatus\":\"" + Optional.ofNullable(hrUser.getHrStatus()).map(Enum::name).orElse("")
                + "\"}";
    }

    private AdminFilterOption option(String label, String value) {
        return new AdminFilterOption(label, value);
    }

    private <E extends Enum<E>> List<AdminFilterOption> enumOptions(E[] values, Function<E, String> labelGetter) {
        return java.util.Arrays.stream(values)
                .map(value -> option(labelGetter.apply(value) + "(" + value.name() + ")", value.name()))
                .toList();
    }
}
