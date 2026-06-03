package com.example.authapp.api.admin;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.authapp.domain.organization.dto.AdminDepartmentRequest;
import com.example.authapp.domain.organization.dto.AdminDepartmentResponse;
import com.example.authapp.domain.organization.dto.AdminDepartmentUserRequest;
import com.example.authapp.domain.organization.dto.AdminDepartmentUserResponse;
import com.example.authapp.domain.organization.service.AdminDepartmentService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/departments")
@RequiredArgsConstructor
public class AdminDepartmentController {

    private final AdminDepartmentService adminDepartmentService;

    @GetMapping
    public List<AdminDepartmentResponse> departments() {
        return adminDepartmentService.list();
    }

    @PostMapping
    public AdminDepartmentResponse create(@RequestBody AdminDepartmentRequest request) {
        return adminDepartmentService.create(request);
    }

    @PatchMapping("/{id}")
    public AdminDepartmentResponse update(@PathVariable Long id, @RequestBody AdminDepartmentRequest request) {
        return adminDepartmentService.update(id, request);
    }

    @PostMapping("/{id}/disable")
    public void disable(
            @PathVariable Long id,
            @RequestParam(name = "reason", required = false) String reason
    ) {
        adminDepartmentService.disable(id, reason);
    }

    @GetMapping("/{id}/users")
    public List<AdminDepartmentUserResponse> users(@PathVariable Long id) {
        return adminDepartmentService.users(id);
    }

    @PostMapping("/{id}/users")
    public AdminDepartmentUserResponse assignUser(
            @PathVariable Long id,
            @RequestBody AdminDepartmentUserRequest request
    ) {
        return adminDepartmentService.assignUser(id, request);
    }

    @PatchMapping("/{id}/users/{username}")
    public AdminDepartmentUserResponse updateUser(
            @PathVariable Long id,
            @PathVariable String username,
            @RequestBody AdminDepartmentUserRequest request
    ) {
        return adminDepartmentService.updateUser(id, username, request);
    }

    @DeleteMapping("/{id}/users/{username}")
    public void removeUser(
            @PathVariable Long id,
            @PathVariable String username,
            @RequestParam(name = "reason", required = false) String reason
    ) {
        adminDepartmentService.removeUser(id, username, reason);
    }
}
