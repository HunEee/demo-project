package com.example.authapp.api.admin;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.authapp.domain.admin.AdminConsoleService;
import com.example.authapp.domain.admin.dto.AdminFilterOptionsResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminFilterOptionsController {

    private final AdminConsoleService adminConsoleService;

    // Admin 필터 dropdowns
    @GetMapping("/filter-options")
    public AdminFilterOptionsResponse filterOptions() {
        return adminConsoleService.filterOptions();
    }

}
