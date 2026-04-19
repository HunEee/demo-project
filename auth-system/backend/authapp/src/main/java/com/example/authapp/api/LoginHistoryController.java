package com.example.authapp.api;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.authapp.domain.audit.dto.LoginHistoryResponseDTO;
import com.example.authapp.domain.audit.dto.PageResponse;
import com.example.authapp.domain.audit.service.LoginHistoryService;
import com.example.authapp.security.principal.UserPrincipal;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/login-histories")
@RequiredArgsConstructor
public class LoginHistoryController {
	
    private final LoginHistoryService loginHistoryService;
    
    @GetMapping
    public PageResponse<LoginHistoryResponseDTO> getLoginHistories(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "date", required = false) String date
    ) {
        return new PageResponse<>(loginHistoryService.getLoginHistories(user.getUsername(), page, size, date));
    }
	
    
    
}
