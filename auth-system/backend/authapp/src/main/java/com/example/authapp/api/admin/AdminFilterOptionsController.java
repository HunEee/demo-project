package com.example.authapp.api.admin;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.authapp.application.admin.usecase.AdminFilterOptionsUseCase;
import com.example.authapp.domain.admin.dto.AdminFilterOptionsResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminFilterOptionsController {

    private final AdminFilterOptionsUseCase adminConsoleService;

    // 관리자 필터 드롭다운 옵션을 조회한다.
    @GetMapping("/filter-options")
    public AdminFilterOptionsResponse filterOptions() {
        return adminConsoleService.filterOptions();
    }

}
