package com.example.authapp.api.admin;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.authapp.domain.authorization.dto.AdminApiPermissionRuleRequest;
import com.example.authapp.domain.authorization.dto.AdminApiPermissionRuleResponse;
import com.example.authapp.domain.authorization.service.AdminApiPermissionRuleService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/api-permission-rules")
@RequiredArgsConstructor
public class AdminApiPermissionRuleController {

    private final AdminApiPermissionRuleService adminApiPermissionRuleService;

    @GetMapping
    public List<AdminApiPermissionRuleResponse> rules() {
        return adminApiPermissionRuleService.list();
    }

    @PostMapping
    public AdminApiPermissionRuleResponse create(@RequestBody AdminApiPermissionRuleRequest request) {
        return adminApiPermissionRuleService.create(request);
    }

    @PatchMapping("/{id}")
    public AdminApiPermissionRuleResponse update(@PathVariable(name = "id") Long id, @RequestBody AdminApiPermissionRuleRequest request) {
        return adminApiPermissionRuleService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable(name = "id") Long id) {
        adminApiPermissionRuleService.delete(id);
    }
}
