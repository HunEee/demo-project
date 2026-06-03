package com.example.authapp.domain.admin.dto;

import java.util.List;

public record AdminFilterOptionsResponse(
        List<AdminFilterOption> userStatuses,
        List<AdminFilterOption> roles,
        List<AdminFilterOption> auditEventTypes,
        List<AdminFilterOption> loginStatuses,
        List<AdminFilterOption> incidentTypes,
        List<AdminFilterOption> incidentSeverities,
        List<AdminFilterOption> sessionStatuses,
        List<AdminFilterOption> riskLevels,
        List<AdminFilterOption> employmentTypes
) {
}
