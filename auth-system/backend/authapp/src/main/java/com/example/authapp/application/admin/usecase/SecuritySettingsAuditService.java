package com.example.authapp.application.admin.usecase;

import org.springframework.stereotype.Service;

import com.example.authapp.domain.audit.entity.AdminActionType;
import com.example.authapp.domain.audit.service.AdminActionLogRequest;
import com.example.authapp.domain.audit.service.AdminActionLogService;
import com.example.authapp.util.ClientUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SecuritySettingsAuditService {

    private final AdminActionLogService adminActionLogService;
    private final ObjectMapper objectMapper;

    public void recordUpdate(String actorUsername, Object beforeData, Object afterData, HttpServletRequest request) {
        adminActionLogService.record(AdminActionLogRequest.builder()
                .actorUsername(actorUsername)
                .targetType("SECURITY_SETTINGS")
                .targetId("1")
                .targetName("SECURITY_SETTINGS")
                .actionType(AdminActionType.SECURITY_POLICY_UPDATED)
                .reason("Update security settings")
                .beforeValue(writeJson(beforeData))
                .afterValue(writeJson(afterData))
                .ipAddress(request == null ? null : ClientUtil.getIp(request))
                .userAgent(request == null ? null : ClientUtil.getUserAgent(request))
                .result("SUCCESS")
                .build());
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return String.valueOf(value);
        }
    }
}
