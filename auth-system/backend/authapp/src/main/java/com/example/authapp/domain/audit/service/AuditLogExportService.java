package com.example.authapp.domain.audit.service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.example.authapp.domain.audit.entity.AdminActionLogEntity;
import com.example.authapp.domain.audit.entity.AdminActionType;
import com.example.authapp.domain.audit.entity.AuditLogExportEntity;
import com.example.authapp.domain.audit.repository.AuditLogExportRepository;
import com.example.authapp.util.ClientUtil;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuditLogExportService {

    private static final int MAX_EXPORT_ROWS = 10_000;

    private final AdminActionLogService adminActionLogService;
    private final AuditLogExportRepository auditLogExportRepository;

    @Transactional
    public AuditExportFile exportAdminActionLogs(
            String actor,
            String target,
            String action,
            String result,
            String reason,
            String riskLevel,
            String ipAddress,
            String userAgent,
            String from,
            String to,
            String sort,
            String direction
    ) {
        List<AdminActionLogEntity> rows = adminActionLogService.search(
                0,
                MAX_EXPORT_ROWS,
                actor,
                target,
                action,
                result,
                reason,
                riskLevel,
                ipAddress,
                userAgent,
                parseStartOfDay(from),
                parseEndOfDay(to),
                sort,
                direction
        ).getContent();

        String filterJson = filterJson(actor, target, action, result, reason, riskLevel, ipAddress, userAgent, from, to);
        HttpServletRequest request = currentRequest();
        String currentUserAgent = request != null ? ClientUtil.getUserAgent(request) : "UNKNOWN";
        String currentIp = request != null ? ClientUtil.getIp(request) : "UNKNOWN";
        String actorUsername = currentActor();

        auditLogExportRepository.save(AuditLogExportEntity.builder()
                .actorUsername(actorUsername)
                .exportType("ADMIN_ACTION_LOG")
                .filterJson(filterJson)
                .rowCount(rows.size())
                .fileFormat("CSV")
                .ipAddress(currentIp)
                .userAgent(currentUserAgent)
                .build());

        adminActionLogService.record(AdminActionLogRequest.builder()
                .actorUsername(actorUsername)
                .targetType("AUDIT_LOG")
                .targetId("ADMIN_ACTION_LOG")
                .actionType(AdminActionType.AUDIT_LOG_EXPORT)
                .result("SUCCESS")
                .reason("Export admin action logs")
                .ipAddress(currentIp)
                .userAgent(currentUserAgent)
                .metadata("{\"filters\":" + filterJson + ",\"rowCount\":" + rows.size() + "}")
                .build());

        return new AuditExportFile("admin-action-logs.csv", toCsv(rows));
    }

    private byte[] toCsv(List<AdminActionLogEntity> rows) {
        StringBuilder csv = new StringBuilder();
        csv.append("createdAt,actor,targetType,targetId,targetUsername,action,result,riskLevel,reason,ipAddress,device,userAgent,beforeValue,afterValue\n");
        for (AdminActionLogEntity row : rows) {
            csv.append(cell(row.getCreatedAt()))
                    .append(',').append(cell(row.getActorUsername()))
                    .append(',').append(cell(row.getTargetType()))
                    .append(',').append(cell(row.getTargetId()))
                    .append(',').append(cell(row.getTargetUsername()))
                    .append(',').append(cell(row.getActionType() != null ? row.getActionType().name() : null))
                    .append(',').append(cell(row.getResult()))
                    .append(',').append(cell(row.getRiskLevel()))
                    .append(',').append(cell(row.getReason()))
                    .append(',').append(cell(row.getIpAddress()))
                    .append(',').append(cell(row.getDevice()))
                    .append(',').append(cell(row.getUserAgent()))
                    .append(',').append(cell(row.getBeforeValue()))
                    .append(',').append(cell(row.getAfterValue()))
                    .append('\n');
        }
        return ("\uFEFF" + csv).getBytes(StandardCharsets.UTF_8);
    }

    private String cell(Object value) {
        if (value == null) return "";
        String text = String.valueOf(value);
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }

    private String filterJson(
            String actor,
            String target,
            String action,
            String result,
            String reason,
            String riskLevel,
            String ipAddress,
            String userAgent,
            String from,
            String to
    ) {
        return "{"
                + "\"actor\":" + json(actor)
                + ",\"target\":" + json(target)
                + ",\"action\":" + json(action)
                + ",\"result\":" + json(result)
                + ",\"reason\":" + json(reason)
                + ",\"riskLevel\":" + json(riskLevel)
                + ",\"ipAddress\":" + json(ipAddress)
                + ",\"userAgent\":" + json(userAgent)
                + ",\"from\":" + json(from)
                + ",\"to\":" + json(to)
                + "}";
    }

    private String json(String value) {
        if (value == null) return "null";
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private LocalDateTime parseStartOfDay(String date) {
        if (date == null || date.isBlank()) return null;
        return LocalDate.parse(date).atStartOfDay();
    }

    private LocalDateTime parseEndOfDay(String date) {
        if (date == null || date.isBlank()) return null;
        return LocalDate.parse(date).plusDays(1).atStartOfDay().minusNanos(1);
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

    public record AuditExportFile(String filename, byte[] bytes) {
    }
}
