package com.lms.config;

import com.lms.service.LocalizedMessageService;
import com.lms.entity.MemberAccount;
import com.lms.repository.MemberAccountRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class InactiveMemberInterceptor implements HandlerInterceptor {

    public static final String INACTIVE_MEMBER_ATTRIBUTE = "isInactiveMember";

    private final LocalizedMessageService messages;
    private final MemberAccountRepository memberAccountRepository;

    public InactiveMemberInterceptor(LocalizedMessageService messages,
                                     MemberAccountRepository memberAccountRepository) {
        this.messages = messages;
        this.memberAccountRepository = memberAccountRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)
                || authentication.getAuthorities().stream()
                .noneMatch(authority -> "ROLE_MEMBER".equals(authority.getAuthority()))) {
            return true;
        }

        MemberAccount currentAccount = memberAccountRepository.findById(userDetails.getAccountId())
                .orElse(null);
        if (currentAccount == null) {
            terminateSession(request);
            response.sendRedirect(request.getContextPath() + "/login?blocked");
            return false;
        }

        String status = currentAccount.getStatus();
        boolean isInactive = "Inactive".equalsIgnoreCase(status);
        request.setAttribute(INACTIVE_MEMBER_ATTRIBUTE, isInactive);

        if ("Blocked".equalsIgnoreCase(status)) {
            terminateSession(request);
            if (expectsJson(request)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN,
                        messages.get("auth.accountLocked"));
            } else {
                response.sendRedirect(request.getContextPath() + "/login?blocked");
            }
            return false;
        }

        if (isInactive && !isSafeMethod(request.getMethod())) {
            if (expectsJson(request)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN,
                        messages.get("member.inactiveActionDenied"));
                return false;
            }
            throw new AccessDeniedException(messages.get("member.inactiveActionDenied"));
        }

        return true;
    }

    private boolean isSafeMethod(String method) {
        return "GET".equalsIgnoreCase(method)
                || "HEAD".equalsIgnoreCase(method)
                || "OPTIONS".equalsIgnoreCase(method);
    }

    private boolean expectsJson(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        return "XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"))
                || (accept != null && accept.contains("application/json"));
    }

    private void terminateSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
    }
}
