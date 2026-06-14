package com.example.authapp.domain.audit.service;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

/**
 * 관리자 작업 로그 서비스
 *
 * 관리자 페이지에서 발생하는 주요 행위들을 기록하고 조회하는 역할을 한다.
 * 예: 사용자 정보 변경, 권한 변경, 계정 잠금, 토큰 회수, MFA 요구 등
 */
@Service
@RequiredArgsConstructor
public class AdminActionLogService {

    //JSON 형태 문자열에서 민감한 값들을 마스킹하기 위한 정규식 {"password":"1234"} -> {"password":"***"}
    private static final Pattern SENSITIVE_JSON_VALUE = Pattern.compile(
            "(?i)(\"(?:password|token|secret|credential|refresh|access|otp|totp|code)[^\"]*\"\\s*:\\s*\")([^\"]*)(\")"
    );
    
    //query string 또는 일반 key=value 형태 문자열에서 민감한 값들을 마스킹하기 위한 정규식 password=1234 -> password=***
    private static final Pattern SENSITIVE_QUERY_VALUE = Pattern.compile(
            "(?i)((?:password|token|secret|credential|refresh|access|otp|totp|code)[^=,&\\s]*\\s*[=:]\\s*)([^,&\\s}]+)"
    );

    private final AdminActionLogRepository repository;

    // 관리자 작업 로그를 저장
    @Transactional
    public AdminActionLogEntity record(AdminActionLogRequest request) {
        // 현재 HTTP 요청 객체 조회
    	HttpServletRequest currentRequest = currentRequest();
        
        // 요청값에 actor, ip, device, userAgent 등이 없으면 현재 SecurityContext와 HttpServletRequest에서 자동으로 보완한다.
        String userAgent = defaultText(request.userAgent(), currentRequest != null ? ClientUtil.getUserAgent(currentRequest) : null);
        
        // beforeValue, afterValue, reason, metadata에는 민감 정보가 들어갈 수 있으므로 저장 전 마스킹한다.
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
                .metadata(mask(request.metadata())) // 추가 메타데이터. 민감 정보 마스킹 후 저장
                .build();
        return repository.save(log);
    }

    // 관리자 작업 로그 목록을 검색
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
            LocalDateTime from,
            LocalDateTime to,
            String sort,
            String direction
    ) {
        // page는 음수가 되지 않도록 보정
        int safePage = Math.max(page, 0);
        // size는 최소 1 이상이 되도록 보정
        int safeSize = Math.max(size, 1);
        
        // 정렬 방향과 정렬 컬럼을 안전하게 정규화한 뒤 Pageable 생성
        Pageable pageable = PageRequest.of(
                safePage,
                safeSize,
                Sort.by(sortDirection(direction), normalizeSort(sort))
        );
        
        // 문자열 action 값을 AdminActionType enum으로 변환
        AdminActionType actionType = parseActionType(action);
        
        // action 값이 입력됐는데 enum 변환에 실패하면 빈 페이지 반환
        if (blankToNull(action) != null && actionType == null) {
            return Page.empty(pageable);
        }
        
        return repository.search(
                blankToNull(actor),
                blankToNull(target),
                actionType,
                blankToNull(result),
                blankToNull(reason),
                blankToNull(riskLevel),
                blankToNull(ipAddress),
                blankToNull(userAgent),
                from,
                to,
                pageable
        );
    }

    // 민감 정보를 마스킹
    public String mask(String value) {
        if (value == null) return null;
        // JSON 형태 민감값 먼저 마스킹
        String masked = SENSITIVE_JSON_VALUE.matcher(value).replaceAll("$1***$3");
        // query string 또는 key=value 형태 민감값 마스킹
        return SENSITIVE_QUERY_VALUE.matcher(masked).replaceAll("$1***");
    }

    // value가 null 또는 blank이면 fallback을 반환
    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    // 문자열이 null 또는 blank이면 null로 변환한다. -> 검색 조건에서 빈 문자열을 조건으로 사용하지 않기 위한 처리
    private String blankToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    // 문자열 action 값을 AdminActionType enum으로 변환 -> 잘못된 값이면 예외를 밖으로 던지지 않고 null을 반환
    private AdminActionType parseActionType(String value) {
        String normalized = blankToNull(value);
        if (normalized == null) return null;
        try {
            return AdminActionType.valueOf(normalized.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // 정령방향
    private Sort.Direction sortDirection(String direction) {
        return "ASC".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
    }

    /**
     * 프론트에서 전달된 sort 값을 실제 엔티티 필드명으로 변환한다.
     *
     * 허용되지 않은 sort 값은 createdAt으로 정렬한다.
     * 이를 통해 잘못된 필드명으로 인한 오류나 의도치 않은 정렬을 방지한다.
     */
    private String normalizeSort(String sort) {
        return switch (sort == null ? "" : sort) {
            case "actor", "actorUsername" -> "actorUsername";
            case "target", "targetUsername" -> "targetUsername";
            case "targetType" -> "targetType";
            case "action", "actionType" -> "actionType";
            case "result" -> "result";
            case "ipAddress" -> "ipAddress";
            case "riskLevel" -> "riskLevel";
            default -> "createdAt";
        };
    }

    // 현재 로그인한 사용자명을 반환 -> SecurityContext에 인증 정보가 없으면 UNKNOWN을 반환
    private String currentActor() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) return "UNKNOWN";
        return authentication.getName();
    }

    //현재 HTTP 요청 객체를 반환한다.
    private HttpServletRequest currentRequest() {
        var attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletRequestAttributes) {
            return servletRequestAttributes.getRequest();
        }
        return null;
    }
}
