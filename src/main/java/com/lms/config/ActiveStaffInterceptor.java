package com.lms.config;

import com.lms.entity.StaffAccount;
import com.lms.repository.StaffAccountRepository;
import com.lms.service.LocalizedMessageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class ActiveStaffInterceptor implements HandlerInterceptor {

    private final StaffAccountRepository staffAccountRepository;
    private final LocalizedMessageService messages;

    public ActiveStaffInterceptor(StaffAccountRepository staffAccountRepository,
                                  LocalizedMessageService messages) {
        this.staffAccountRepository = staffAccountRepository;
        this.messages = messages;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)
                || authentication.getAuthorities().stream().noneMatch(authority ->
                        "ROLE_ADMIN".equals(authority.getAuthority())
                                || "ROLE_LIBRARIAN".equals(authority.getAuthority()))) {
            return true;
        }

        StaffAccount account = staffAccountRepository.findById(userDetails.getAccountId()).orElse(null);
        if (account != null && "Active".equalsIgnoreCase(account.getStatus())) {
            return true;
        }

        terminateSession(request);
        if (expectsJson(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, messages.get("backend.account.disabled"));
        } else {
            response.sendRedirect(request.getContextPath() + "/staff-login?disabled");
        }
        return false;
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
