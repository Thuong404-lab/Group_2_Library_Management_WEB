package com.lms.controller.auth;

import com.lms.config.CustomUserDetails;
import com.lms.entity.SystemLog;
import com.lms.enums.ActionType;
import com.lms.repository.SystemLogRepository;
import com.lms.service.AuditLogService;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;

/**
 * Allows members to inspect and revoke their own authenticated sessions.
 */
@Controller
@RequestMapping("/member/sessions")
public class SessionController {

    private static final List<String> AUTH_ACTIONS = List.of(
            ActionType.LOGIN.name(), ActionType.LOGOUT.name(),
            ActionType.GOOGLE.name(), ActionType.REVOKE_SESSION.name());

    private final SessionRegistry sessionRegistry;
    private final SystemLogRepository systemLogRepository;
    private final AuditLogService auditLogService;

    public SessionController(SessionRegistry sessionRegistry,
            SystemLogRepository systemLogRepository,
            AuditLogService auditLogService) {
        this.sessionRegistry = sessionRegistry;
        this.systemLogRepository = systemLogRepository;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public String viewActiveSessions(Authentication authentication, HttpSession currentSession, Model model) {
        List<SessionInformation> sessions = sessionRegistry
                .getAllSessions(authentication.getPrincipal(), false)
                .stream()
                .sorted(Comparator.comparing(SessionInformation::getLastRequest).reversed())
                .toList();
        model.addAttribute("sessions", sessions);
        model.addAttribute("currentSessionId", currentSession.getId());
        return "member/active-sessions";
    }

    @PostMapping("/revoke")
    public String revokeSession(@RequestParam String sessionId,
            Authentication authentication,
            HttpSession currentSession) {
        SessionInformation target = sessionRegistry.getSessionInformation(sessionId);
        if (target == null || !target.getPrincipal().equals(authentication.getPrincipal())) {
            return "redirect:/member/sessions?invalid";
        }
        auditLogService.log(ActionType.REVOKE_SESSION, "Revoked login session " + sessionId);
        target.expireNow();
        if (sessionId.equals(currentSession.getId())) {
            currentSession.invalidate();
            return "redirect:/login?logout";
        }
        return "redirect:/member/sessions?revoked";
    }

    @GetMapping("/history")
    public String viewLoginHistory(Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
        Page<SystemLog> history = systemLogRepository
                .findByUser_IdAndActionTypeInOrderByCreatedAtDesc(
                        principal.getUser().getId(), AUTH_ACTIONS,
                        PageRequest.of(Math.max(page, 0), 20));
        model.addAttribute("history", history);
        return "member/login-history";
    }
}
