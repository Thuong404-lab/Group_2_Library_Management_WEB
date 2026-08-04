package com.lms.controller.auth;

import com.lms.config.CustomUserDetails;
import com.lms.entity.SystemLog;
import com.lms.entity.User;
import com.lms.repository.SystemLogRepository;
import com.lms.service.AuditLogService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.ui.ExtendedModelMap;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SessionControllerTest {

    @Test
    void activeSessionsOnlyComeFromAuthenticatedPrincipal() {
        SessionRegistry registry = mock(SessionRegistry.class);
        Object principal = new Object();
        SessionInformation information = new SessionInformation(principal, "session-1", new Date());
        when(registry.getAllSessions(principal, false)).thenReturn(List.of(information));
        SessionController controller = controller(registry, mock(SystemLogRepository.class), mock(AuditLogService.class));
        ExtendedModelMap model = new ExtendedModelMap();
        MockHttpSession session = new MockHttpSession();

        String view = controller.viewActiveSessions(
                new TestingAuthenticationToken(principal, null), session, model);

        assertThat(view).isEqualTo("member/active-sessions");
        assertThat(model.get("sessions")).isEqualTo(List.of(information));
        assertThat(model.get("currentSessionId")).isEqualTo(session.getId());
    }

    @Test
    void cannotRevokeAnotherUsersSession() {
        SessionRegistry registry = mock(SessionRegistry.class);
        AuditLogService audit = mock(AuditLogService.class);
        Object currentPrincipal = new Object();
        when(registry.getSessionInformation("foreign"))
                .thenReturn(new SessionInformation(new Object(), "foreign", new Date()));
        SessionController controller = controller(registry, mock(SystemLogRepository.class), audit);

        String redirect = controller.revokeSession("foreign",
                new TestingAuthenticationToken(currentPrincipal, null), new MockHttpSession());

        assertThat(redirect).isEqualTo("redirect:/member/sessions?invalid");
        verify(audit, never()).log(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void loginHistoryIsRestrictedToAuthenticatedUser() {
        SessionRegistry registry = mock(SessionRegistry.class);
        SystemLogRepository logs = mock(SystemLogRepository.class);
        User user = new User();
        user.setId(42);
        CustomUserDetails principal = new CustomUserDetails(user, "member", "", "Active", 7, List.of());
        when(logs.findByUser_IdAndActionTypeInOrderByCreatedAtDesc(
                eq(42), anyList(), org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(new PageImpl<SystemLog>(List.of()));
        SessionController controller = controller(registry, logs, mock(AuditLogService.class));

        String view = controller.viewLoginHistory(
                new TestingAuthenticationToken(principal, null), -5, new ExtendedModelMap());

        assertThat(view).isEqualTo("member/login-history");
        verify(logs).findByUser_IdAndActionTypeInOrderByCreatedAtDesc(
                eq(42), anyList(), org.mockito.ArgumentMatchers.argThat(page -> page.getPageNumber() == 0));
    }

    private SessionController controller(SessionRegistry registry, SystemLogRepository logs, AuditLogService audit) {
        return new SessionController(registry, logs, audit);
    }
}
