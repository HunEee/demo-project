package com.example.authapp.api.admin;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.authapp.domain.admin.AdminConsoleService;
import com.example.authapp.domain.admin.dto.AdminSessionResponse;
import com.example.authapp.domain.audit.dto.PageResponseDTO;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/sessions")
@RequiredArgsConstructor
public class AdminSessionController {

    private final AdminConsoleService adminConsoleService;

    // 전체 사용자 세션/토큰을 조회
    @GetMapping
    public PageResponseDTO<AdminSessionResponse> sessions(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "username", required = false) String username,
            @RequestParam(name = "activeOnly", required = false) Boolean activeOnly,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "device", required = false) String device,
            @RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "to", required = false) String to,
            @RequestParam(name = "sort", required = false) String sort,
            @RequestParam(name = "direction", defaultValue = "DESC") String direction
    ) {
        return new PageResponseDTO<>(adminConsoleService.sessions(page, size, username, activeOnly, status, device, from, to, sort, direction));
    }

    // 특정 refresh token 세션을 폐기
    @DeleteMapping("/{id}")
    public void revoke(@PathVariable Long id) {
        adminConsoleService.revokeSession(id);
    }

}
