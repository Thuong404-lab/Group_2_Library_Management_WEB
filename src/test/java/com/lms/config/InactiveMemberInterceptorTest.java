package com.lms.config;

import com.lms.entity.MemberAccount;
import com.lms.repository.MemberAccountRepository;
import com.lms.service.LocalizedMessageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InactiveMemberInterceptorTest {

    private final MemberAccountRepository accountRepository = mock(MemberAccountRepository.class);
    private final LocalizedMessageService messages = mock(LocalizedMessageService.class);
    private final InactiveMemberInterceptor interceptor =
            new InactiveMemberInterceptor(messages, accountRepository);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void inactiveMemberMayViewPages() throws Exception {
        authenticateMember(7);
        when(accountRepository.findById(7)).thenReturn(Optional.of(account("Inactive")));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/member/profile");

        boolean allowed = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertThat(allowed).isTrue();
        assertThat(request.getAttribute(InactiveMemberInterceptor.INACTIVE_MEMBER_ATTRIBUTE)).isEqualTo(true);
    }

    @Test
    void inactiveMemberCannotSubmitMutation() {
        authenticateMember(7);
        when(accountRepository.findById(7)).thenReturn(Optional.of(account("Inactive")));
        when(messages.get("member.inactiveActionDenied")).thenReturn("Read-only account");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/member/profile");

        assertThatThrownBy(() -> interceptor.preHandle(
                request, new MockHttpServletResponse(), new Object()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Read-only account");
    }

    @Test
    void inactiveMemberMayPayFineFromWallet() throws Exception {
        authenticateMember(7);
        when(accountRepository.findById(7)).thenReturn(Optional.of(account("Inactive")));
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST", "/member/financial/fines/pay/42");

        boolean allowed = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertThat(allowed).isTrue();
    }

    @Test
    void inactiveMemberMayCreateFinePaymentAndTopUp() throws Exception {
        authenticateMember(7);
        when(accountRepository.findById(7)).thenReturn(Optional.of(account("Inactive")));

        MockHttpServletRequest finePayment = new MockHttpServletRequest(
                "POST", "/member/payments/payos/fine/all");
        MockHttpServletRequest topUp = new MockHttpServletRequest(
                "POST", "/member/payments/payos/top-up");

        assertThat(interceptor.preHandle(finePayment, new MockHttpServletResponse(), new Object())).isTrue();
        assertThat(interceptor.preHandle(topUp, new MockHttpServletResponse(), new Object())).isTrue();
    }

    @Test
    void inactiveMemberCannotUseSimilarButUnapprovedPaymentPath() {
        authenticateMember(7);
        when(accountRepository.findById(7)).thenReturn(Optional.of(account("Inactive")));
        when(messages.get("member.inactiveActionDenied")).thenReturn("Restricted account");
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST", "/member/payments/payos/fine/not-a-valid-id");

        assertThatThrownBy(() -> interceptor.preHandle(
                request, new MockHttpServletResponse(), new Object()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Restricted account");
    }

    @Test
    void inactiveMemberMayPayExistingFine() throws Exception {
        authenticateMember(7);
        when(accountRepository.findById(7)).thenReturn(Optional.of(account("Inactive")));
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST", "/member/payments/payos/fine/15");

        assertThat(interceptor.preHandle(request, new MockHttpServletResponse(), new Object())).isTrue();
    }

    @Test
    void inactiveMemberMayMarkNotificationRead() throws Exception {
        authenticateMember(7);
        when(accountRepository.findById(7)).thenReturn(Optional.of(account("Inactive")));
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST", "/member/interaction/notifications/25/mark-read");

        assertThat(interceptor.preHandle(request, new MockHttpServletResponse(), new Object())).isTrue();
    }

    @Test
    void blockedMemberSessionIsTerminatedAndRedirected() throws Exception {
        authenticateMember(7);
        when(accountRepository.findById(7)).thenReturn(Optional.of(account("Blocked")));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/member/profile");
        request.getSession(true);
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertThat(allowed).isFalse();
        assertThat(response.getRedirectedUrl()).isEqualTo("/login?blocked");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void inactiveCredentialsRemainEnabledForReadOnlyLogin() {
        CustomUserDetails inactive = userDetails("Inactive");

        assertThat(inactive.isEnabled()).isTrue();
        assertThat(inactive.isAccountNonLocked()).isTrue();
    }

    @Test
    void blockedCredentialsCannotLogin() {
        CustomUserDetails blocked = userDetails("Blocked");

        assertThat(blocked.isEnabled()).isFalse();
        assertThat(blocked.isAccountNonLocked()).isFalse();
    }

    private void authenticateMember(Integer accountId) {
        CustomUserDetails principal = new CustomUserDetails(
                null,
                "member01",
                "encoded",
                "Active",
                accountId,
                List.of(new SimpleGrantedAuthority("ROLE_MEMBER")));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private CustomUserDetails userDetails(String status) {
        return new CustomUserDetails(
                null,
                "member01",
                "encoded",
                status,
                7,
                List.of(new SimpleGrantedAuthority("ROLE_MEMBER")));
    }

    private MemberAccount account(String status) {
        MemberAccount account = new MemberAccount();
        account.setId(7);
        account.setStatus(status);
        return account;
    }
}
