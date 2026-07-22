package com.lms.config;

import com.lms.entity.StaffAccount;
import com.lms.repository.StaffAccountRepository;
import com.lms.service.LocalizedMessageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ActiveStaffInterceptorTest {

    private final StaffAccountRepository accountRepository = mock(StaffAccountRepository.class);
    private final LocalizedMessageService messages = mock(LocalizedMessageService.class);
    private final ActiveStaffInterceptor interceptor = new ActiveStaffInterceptor(accountRepository, messages);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void activeStaffRequestIsAllowed() throws Exception {
        authenticateStaff(8);
        when(accountRepository.findById(8)).thenReturn(Optional.of(account("Active")));

        boolean allowed = interceptor.preHandle(
                new MockHttpServletRequest("GET", "/librarian/profile"),
                new MockHttpServletResponse(), new Object());

        assertThat(allowed).isTrue();
    }

    @Test
    void inactiveStaffSessionIsTerminatedImmediately() throws Exception {
        authenticateStaff(8);
        when(accountRepository.findById(8)).thenReturn(Optional.of(account("Inactive")));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/librarian/profile");
        request.getSession(true);
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertThat(allowed).isFalse();
        assertThat(response.getRedirectedUrl()).isEqualTo("/staff-login?disabled");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void inactiveStaffCredentialsAreDisabledButInactiveMemberCredentialsRemainEnabled() {
        CustomUserDetails staff = userDetails("Inactive", false, "ROLE_LIBRARIAN");
        CustomUserDetails member = userDetails("Inactive", true, "ROLE_MEMBER");

        assertThat(staff.isEnabled()).isFalse();
        assertThat(member.isEnabled()).isTrue();
    }

    private void authenticateStaff(Integer accountId) {
        CustomUserDetails principal = userDetails("Active", false, "ROLE_LIBRARIAN", accountId);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private CustomUserDetails userDetails(String status, boolean inactiveAllowed, String role) {
        return userDetails(status, inactiveAllowed, role, 8);
    }

    private CustomUserDetails userDetails(String status, boolean inactiveAllowed, String role, Integer accountId) {
        return new CustomUserDetails(null, "staff01", "encoded", status, accountId, inactiveAllowed,
                List.of(new SimpleGrantedAuthority(role)));
    }

    private StaffAccount account(String status) {
        StaffAccount account = new StaffAccount();
        account.setId(8);
        account.setStatus(status);
        return account;
    }
}
