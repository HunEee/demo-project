package com.example.authapp.api.admin;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.authapp.domain.admin.dto.AdminDuplicateCheckResponse;
import com.example.authapp.domain.hr.dto.HrUserMasterRequest;
import com.example.authapp.domain.hr.dto.HrUserMasterResponse;
import com.example.authapp.domain.hr.service.HrUserMasterService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/hr-users")
@RequiredArgsConstructor
public class AdminHrUserMasterController {

    private final HrUserMasterService hrUserMasterService;

    @GetMapping
    public List<HrUserMasterResponse> list(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "accountStatus", required = false) String accountStatus
    ) {
        return hrUserMasterService.list(keyword, accountStatus);
    }

    @GetMapping("/candidates")
    public List<HrUserMasterResponse> accountCandidates() {
        return hrUserMasterService.accountCandidates();
    }

    @GetMapping({"/duplicate-check", "/exists"})
    public AdminDuplicateCheckResponse exists(
            @RequestParam(name = "field") String field,
            @RequestParam(name = "value") String value
    ) {
        return new AdminDuplicateCheckResponse(field, value, hrUserMasterService.exists(field, value));
    }

    @PostMapping
    public HrUserMasterResponse create(@RequestBody HrUserMasterRequest request) {
        return hrUserMasterService.create(request);
    }

    @PatchMapping("/{id}")
    public HrUserMasterResponse update(@PathVariable(name = "id") Long id, @RequestBody HrUserMasterRequest request) {
        return hrUserMasterService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable(name = "id") Long id) {
        hrUserMasterService.delete(id);
    }
}
