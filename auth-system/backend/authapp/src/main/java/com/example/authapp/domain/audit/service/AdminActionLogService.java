package com.example.authapp.domain.audit.service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.example.authapp.domain.audit.entity.AdminActionLogEntity;
import com.example.authapp.domain.audit.entity.AdminActionType;
import com.example.authapp.domain.audit.repository.AdminActionLogRepository;
import com.example.authapp.util.ClientUtil;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminActionLogService {

    private static final Pattern SENSITIVE_JSON_VALUE = Pattern.compile(
            "(?i)(\"(?:password|token|secret|credential|refresh|access|otp|totp|code)[^\"]*\"\\s*:\\s*\")([^\"]*)(\")"
    );
    private static final Pattern SENSITIVE_QUERY_VALUE = Pattern.compile(
            "(?i)((?:password|token|secret|credential|refresh|access|otp|totp|code)[^=,&\\s]*\\s*[=:]\\s*)([^,&\\s}]+)"
    );

    private final AdminActionLogRepository repository;

    @Transactional
    public AdminActionLogEntity record(AdminActionLogRequest request) {
        HttpServletRequest currentRequest = currentRequest();
        String userAgent = defaultText(request.userAgent(), currentRequest != null ? ClientUtil.getUserAgent(currentRequest) : null);

        AdminActionLogEntity log = AdminActionLogEntity.builder()
                .actorUsername(defaultText(request.actorUsername(), currentActor()))
                .targetType(defaultText(request.targetType(), "UNKNOWN"))
                .targetId(request.targetId())
                .targetUsername(request.targetUsername())
                .targetName(request.targetName())
                .actionType(Optional.ofNullable(request.actionType()).orElse(AdminActionType.PROFILE_UPDATE))
                .reason(mask(request.reason()))
                .beforeValue(mask(request.beforeValue()))
                .afterValue(mask(request.afterValue()))
                .ipAddress(defaultText(request.ipAddress(), currentRequest != null ? ClientUtil.getIp(currentRequest) : "UNKNOWN"))
                .device(defaultText(request.device(), userAgent != null ? ClientUtil.getDevice(userAgent) : "UNKNOWN"))
                .userAgent(userAgent)
                .result(defaultText(request.result(), "SUCCESS"))
                .riskLevel(request.riskLevel())
                .metadata(mask(request.metadata()))
                .build();
        return repository.save(log);
    }

    @Transactional(readOnly = true)
    public Page<AdminActionLogEntity> search(
            int page,
            int size,
            String actor,
            String target,
            String action,
            String result,
            String reason,
            String riskLevel,
            String ipAddress,
            String userAgent,
            java.time.LocalDateTime from,
            java.time.LocalDateTime to,
            String sort,
            String direction
    ) {
        List<AdminActionLogEntity> content = repository.findAll().stream()
                .filter(log -> contains(log.getActorUsername(), actor))
                .filter(log -> targetMatches(log, target))
                .filter(log -> action == null || action.isBlank() || log.getActionType().name().equalsIgnoreCase(action))
                .filter(log -> contains(log.getResult(), result))
                .filter(log -> contains(log.getReason(), reason))
                .filter(log -> contains(log.getRiskLevel(), riskLevel))
                .filter(log -> contains(log.getIpAddress(), ipAddress))
                .filter(log -> contains(log.getUserAgent(), userAgent) || contains(log.getDevice(), userAgent))
                .filter(log -> log.getCreatedAt() == null || from == null || !log.getCreatedAt().isBefore(from))
                .filter(log -> log.getCreatedAt() == null || to == null || !log.getCreatedAt().isAfter(to))
                .sorted(applyDirection(comparator(sort), direction))
                .toList();

        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);
        int start = Math.min(safePage * safeSize, content.size());
        int end = Math.min(start + safeSize, content.size());
        Pageable pageable = PageRequest.of(safePage, safeSize);
        return new PageImpl<>(content.subList(start, end), pageable, content.size());
    }

    public String mask(String value) {
        if (value == null) return null;
        String masked = SENSITIVE_JSON_VALUE.matcher(value).replaceAll("$1***$3");
        return SENSITIVE_QUERY_VALUE.matcher(masked).replaceAll("$1***");
    }

    private boolean targetMatches(AdminActionLogEntity log, String target) {
        return contains(log.getTargetType(), target)
                || contains(log.getTargetId(), target)
                || contains(log.getTargetUsername(), target)
                || contains(log.getTargetName(), target);
    }

    private boolean contains(String value, String keyword) {
        if (keyword == null || keyword.isBlank()) return true;
        if (value == null) return false;
        return value.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT));
    }

    private java.util.Comparator<AdminActionLogEntity> comparator(String sort) {
        return switch (sort == null ? "" : sort) {
            case "actorUsername", "actor" -> compareText(AdminActionLogEntity::getActorUsername);
            case "targetType" -> compareText(AdminActionLogEntity::getTargetType);
            case "targetUsername", "target" -> compareText(AdminActionLogEntity::getTargetUsername);
            case "actionType", "action" -> compareText(log -> log.getActionType().name());
            case "result" -> compareText(AdminActionLogEntity::getResult);
            case "ipAddress" -> compareText(AdminActionLogEntity::getIpAddress);
            case "riskLevel" -> compareText(AdminActionLogEntity::getRiskLevel);
            default -> compareValue(AdminActionLogEntity::getCreatedAt);
        };
    }

    private java.util.Comparator<AdminActionLogEntity> applyDirection(
            java.util.Comparator<AdminActionLogEntity> comparator,
            String direction
    ) {
        return "ASC".equalsIgnoreCase(direction) ? comparator : comparator.reversed();
    }

    private java.util.Comparator<AdminActionLogEntity> compareText(Function<AdminActionLogEntity, String> getter) {
        return java.util.Comparator.comparing(getter, java.util.Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
    }

    private <U extends Comparable<? super U>> java.util.Comparator<AdminActionLogEntity> compareValue(Function<AdminActionLogEntity, U> getter) {
        return java.util.Comparator.comparing(getter, java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()));
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
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
}
