package com.example.authapp.api.admin;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.authapp.domain.authorization.dto.AdminRoleAssignmentHistoryResponse;
import com.example.authapp.domain.authorization.service.RoleAssignmentService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/role-assignment-history")
@RequiredArgsConstructor
public class AdminRoleAssignmentHistoryController {

    private final RoleAssignmentService roleAssignmentService;

    @GetMapping
    public List<AdminRoleAssignmentHistoryResponse> history() {
        return roleAssignmentService.history();
    }
}
