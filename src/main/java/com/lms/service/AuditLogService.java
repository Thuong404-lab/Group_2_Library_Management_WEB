package com.lms.service;

import com.lms.config.CustomUserDetails;
import com.lms.entity.SystemLog;
import com.lms.entity.User;
import com.lms.enums.ActionType;
import com.lms.repository.SystemLogRepository;
import com.lms.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class AuditLogService {

    private final SystemLogRepository systemLogRepository;
    private final UserRepository userRepository;

    public AuditLogService(SystemLogRepository systemLogRepository,
                           UserRepository userRepository) {
        this.systemLogRepository = systemLogRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void log(ActionType actionType, String description) {
        User actor = getCurrentUser();
        HttpServletRequest request = getCurrentRequest();
        String ipAddress = request == null ? null : request.getRemoteAddr();
        String userAgent = request == null ? null : request.getHeader("User-Agent");

        systemLogRepository.save(new SystemLog(
                actor,
                actionType.name(),
                ipAddress,
                userAgent,
                description));
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)
                || userDetails.getUser() == null
                || userDetails.getUser().getId() == null) {
            return null;
        }

        return userRepository.findById(userDetails.getUser().getId()).orElse(null);
    }

    private HttpServletRequest getCurrentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }
}
