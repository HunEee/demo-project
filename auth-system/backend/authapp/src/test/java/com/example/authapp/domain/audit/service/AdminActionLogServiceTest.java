package com.example.authapp.domain.audit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.example.authapp.domain.audit.entity.AdminActionLogEntity;
import com.example.authapp.domain.audit.entity.AdminActionType;
import com.example.authapp.domain.audit.repository.AdminActionLogRepository;

@ExtendWith(MockitoExtension.class)
class AdminActionLogServiceTest {

    @Mock
    private AdminActionLogRepository repository;

    @Test
    void searchDelegatesFiltersToRepositoryQuery() {
        AdminActionLogService service = new AdminActionLogService(repository);
        AdminActionLogEntity log = AdminActionLogEntity.builder()
                .actorUsername("admin1")
                .targetType("USER")
                .targetId("alice")
                .targetUsername("alice")
                .actionType(AdminActionType.UPDATE_USER)
                .ipAddress("10.0.0.1")
                .result("SUCCESS")
                .build();
        when(repository.search(
                eq("admin1"),
                eq("alice"),
                eq(AdminActionType.UPDATE_USER),
                eq("SUCCESS"),
                isNull(),
                isNull(),
                eq("10.0.0.1"),
                isNull(),
                any(),
                any(),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(log)));

        LocalDateTime from = LocalDateTime.of(2026, 6, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 6, 13, 23, 59);
        var result = service.search(
                0,
                20,
                " admin1 ",
                " alice ",
                "UPDATE_USER",
                "SUCCESS",
                "",
                "",
                "10.0.0.1",
                "",
                from,
                to,
                "createdAt",
                "DESC"
        );

        assertThat(result.getContent()).containsExactly(log);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).search(
                eq("admin1"),
                eq("alice"),
                eq(AdminActionType.UPDATE_USER),
                eq("SUCCESS"),
                isNull(),
                isNull(),
                eq("10.0.0.1"),
                isNull(),
                eq(from),
                eq(to),
                pageableCaptor.capture()
        );
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(20);
    }

    @Test
    void searchReturnsEmptyPageForUnknownActionFilter() {
        AdminActionLogService service = new AdminActionLogService(repository);

        var result = service.search(
                0,
                20,
                null,
                null,
                "NOT_A_REAL_ACTION",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "createdAt",
                "DESC"
        );

        assertThat(result.getContent()).isEmpty();
        verifyNoInteractions(repository);
    }
}
