package com.example.authapp.security.rbac;

import java.util.function.Supplier;

import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@SuppressWarnings("deprecation")
public class RbacAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    private final RbacAuthorizationService rbacAuthorizationService;

    @Override
    public AuthorizationDecision check(Supplier<Authentication> authentication, RequestAuthorizationContext context) {
        HttpServletRequest request = context.getRequest();
        String requestPath = resolveRequestPath(request);
        boolean allowed = rbacAuthorizationService.isAllowed(authentication.get(), request.getMethod(), requestPath);
        return new AuthorizationDecision(allowed);
    }

    private String resolveRequestPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isBlank() && uri.startsWith(contextPath)) {
            return uri.substring(contextPath.length());
        }
        return uri;
    }
}
