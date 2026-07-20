package com.lms.controller.advice;

import com.lms.service.MemberNotificationService;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.security.Principal;

@ControllerAdvice
public class MemberNotificationModelAdvice {

    private final MemberNotificationService memberNotificationService;

    public MemberNotificationModelAdvice(MemberNotificationService memberNotificationService) {
        this.memberNotificationService = memberNotificationService;
    }

    @ModelAttribute
    public void addMemberNotificationData(Model model,
                                          Principal principal,
                                          Authentication authentication) {

        model.addAttribute("latestNotifications", null);
        model.addAttribute("showNotificationBell", false);
        model.addAttribute("unreadNotificationCount", 0L);
        model.addAttribute("isInactiveMember", false);

        if (principal == null || authentication == null) {
            return;
        }

        boolean isMember = authentication.getAuthorities()
                .stream()
                .anyMatch(auth -> "ROLE_MEMBER".equals(auth.getAuthority()));

        if (!isMember) {
            return;
        }

        model.addAttribute("showNotificationBell", true);
        
        if (authentication.getPrincipal() instanceof com.lms.config.CustomUserDetails userDetails) {
            boolean isInactive = "Inactive".equalsIgnoreCase(userDetails.getUser().getStatus().name());
            model.addAttribute("isInactiveMember", isInactive);
        }

        model.addAttribute(
                "latestNotifications",
                memberNotificationService.getLatestNotifications(principal.getName())
        );
        model.addAttribute(
                "unreadNotificationCount",
                memberNotificationService.countUnreadNotifications(principal.getName())
        );
    }
}
