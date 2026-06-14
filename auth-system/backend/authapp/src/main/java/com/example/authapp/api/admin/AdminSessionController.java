package com.example.authapp.api.admin;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.authapp.application.admin.usecase.AdminSessionUseCase;
import com.example.authapp.domain.admin.dto.AdminSessionResponse;
import com.example.authapp.domain.audit.dto.PageResponseDTO;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/sessions")
@RequiredArgsConstructor
public class AdminSessionController {

    private final AdminSessionUseCase adminConsoleService;

    // 전체 사용자 세션과 토큰을 조회한다.
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

    // 지정한 refresh token 세션을 폐기한다.
    @DeleteMapping("/{id}")
    public void revoke(@PathVariable(name = "id") Long id) {
        adminConsoleService.revokeSession(id);
    }

}
