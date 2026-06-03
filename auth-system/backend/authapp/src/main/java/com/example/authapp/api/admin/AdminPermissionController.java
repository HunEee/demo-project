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

import com.example.authapp.domain.authorization.dto.AdminPermissionRequest;
import com.example.authapp.domain.authorization.dto.AdminPermissionResponse;
import com.example.authapp.domain.authorization.service.AdminPermissionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/permissions")
@RequiredArgsConstructor
public class AdminPermissionController {

    private final AdminPermissionService adminPermissionService;

    @GetMapping
    public List<AdminPermissionResponse> permissions() {
        return adminPermissionService.list();
    }

    @PostMapping
    public AdminPermissionResponse create(@RequestBody AdminPermissionRequest request) {
        return adminPermissionService.create(request);
    }

    @PatchMapping("/{id}")
    public AdminPermissionResponse update(@PathVariable Long id, @RequestBody AdminPermissionRequest request) {
        return adminPermissionService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        adminPermissionService.delete(id);
    }
}
