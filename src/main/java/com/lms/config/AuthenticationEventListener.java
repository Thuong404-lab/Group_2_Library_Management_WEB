package com.lms.config;

import com.lms.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.LogoutSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class AuthenticationEventListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationEventListener.class);
    private final AuthService authService;

    public AuthenticationEventListener(AuthService authService) {
        this.authService = authService;
    }

    @EventListener
    public void handleAuthenticationSuccessEvent(AuthenticationSuccessEvent event) {
        Object principal = event.getAuthentication().getPrincipal();
        if (principal instanceof CustomUserDetails userDetails) {
            Integer userId = userDetails.getUser().getId();
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
                    .getRequestAttributes();

            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String ipAddress = request.getRemoteAddr();
                String userAgent = request.getHeader("User-Agent");
                
                if (userDetails.getAttributes() != null) {
                    safelyLog("Google login", () -> authService.logGoogleLoginAction(userId, ipAddress, userAgent));
                } else {
                    safelyLog("staff login", () -> authService.logLoginAction(userId, ipAddress, userAgent));
                }
            }
        }
    }

    @EventListener
    public void handleLogoutSuccessEvent(LogoutSuccessEvent event) {
        Object principal = event.getAuthentication().getPrincipal();

        if (principal instanceof CustomUserDetails userDetails) {
            Integer userId = userDetails.getUser().getId();
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
                    .getRequestAttributes();

            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String ipAddress = request.getRemoteAddr();
                String userAgent = request.getHeader("User-Agent");
                safelyLog("logout", () -> authService.logLogoutAction(userId, ipAddress, userAgent));
            }
        }
    }

    private void safelyLog(String action, Runnable logAction) {
        try {
            logAction.run();
        } catch (RuntimeException exception) {
            LOGGER.error("Unable to persist {} audit event; authentication flow will continue.", action, exception);
        }
    }

}
