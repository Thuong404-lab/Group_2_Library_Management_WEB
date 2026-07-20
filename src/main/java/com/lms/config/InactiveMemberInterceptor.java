package com.lms.config;

import com.lms.config.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class InactiveMemberInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String method = request.getMethod();
        String uri = request.getRequestURI();

        // Check if the endpoint starts with /member/ or /api/
        if (uri.startsWith("/member") || uri.startsWith("/api")) {
            
            // Allow inactive members to view their data (GET only)
            if ("GET".equalsIgnoreCase(method)) {
                // Prevent state-changing GET requests
                if (!uri.contains("mark-read")) {
                    return true;
                }
            }

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof CustomUserDetails userDetails) {
                boolean isMember = auth.getAuthorities().stream()
                        .anyMatch(a -> "ROLE_MEMBER".equals(a.getAuthority()));
                
                if (isMember) {
                    boolean isInactive = "Inactive".equalsIgnoreCase(userDetails.getUser().getStatus().name());
                    if (isInactive) {
                        throw new AccessDeniedException("Tài khoản bạn đang bị dừng hoạt động.");
                    }
                }
            }
        }
        return true;
    }
}
