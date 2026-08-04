package com.lms.service;

import com.lms.entity.Member;
import com.lms.entity.MemberAccount;
import com.lms.entity.Role;
import com.lms.entity.Staff;
import com.lms.entity.StaffAccount;
import com.lms.entity.User;
import com.lms.repository.MemberAccountRepository;
import com.lms.repository.StaffAccountRepository;
import com.lms.service.impl.CustomMemberDetailsService;
import com.lms.service.impl.CustomStaffDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AccountRoleBoundaryUserDetailsTest {

    @Test
    void memberCannotInheritAdminRoleFromCorruptedMapping() {
        MemberAccountRepository repository = mock(MemberAccountRepository.class);
        MemberAccount account = memberAccount("member01");
        account.setRoles(Set.of(new Role(1, "ROLE_ADMIN")));
        when(repository.findByUsername("member01")).thenReturn(Optional.of(account));

        var details = new CustomMemberDetailsService(repository).loadUserByUsername("member01");

        assertThat(details.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_MEMBER");
    }

    @Test
    void librarianCannotInheritAdminRoleFromCorruptedMapping() {
        StaffAccountRepository repository = mock(StaffAccountRepository.class);
        StaffAccount account = staffAccount("librarian01", "Librarian");
        account.setRoles(Set.of(new Role(1, "ROLE_ADMIN")));
        when(repository.findByUsername("librarian01")).thenReturn(Optional.of(account));

        var details = new CustomStaffDetailsService(repository, mock(LocalizedMessageService.class))
                .loadUserByUsername("librarian01");

        assertThat(details.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_LIBRARIAN");
    }

    @Test
    void adminAuthorityComesFromAdminStaffType() {
        StaffAccountRepository repository = mock(StaffAccountRepository.class);
        StaffAccount account = staffAccount("admin", "Admin");
        account.setRoles(Set.of(new Role(3, "ROLE_MEMBER")));
        when(repository.findByUsername("admin")).thenReturn(Optional.of(account));

        var details = new CustomStaffDetailsService(repository, mock(LocalizedMessageService.class))
                .loadUserByUsername("admin");

        assertThat(details.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    void unknownStaffTypeIsDenied() {
        StaffAccountRepository repository = mock(StaffAccountRepository.class);
        StaffAccount account = staffAccount("broken", "Member");
        when(repository.findByUsername("broken")).thenReturn(Optional.of(account));
        LocalizedMessageService messages = mock(LocalizedMessageService.class);
        when(messages.get("backend.account.staffUsernameNotFound", "broken"))
                .thenReturn("Staff account unavailable");

        CustomStaffDetailsService service = new CustomStaffDetailsService(repository, messages);

        assertThatThrownBy(() -> service.loadUserByUsername("broken"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void oauthMemberDropsAnyProviderSuppliedRole() {
        var authorities = CustomOAuth2UserService.memberAuthorities(List.of(
                new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"),
                new org.springframework.security.core.authority.SimpleGrantedAuthority("SCOPE_email")));

        assertThat(authorities)
                .extracting("authority")
                .containsExactly("SCOPE_email", "ROLE_MEMBER");
    }

    private MemberAccount memberAccount(String username) {
        User user = new User();
        Member member = new Member();
        member.setUser(user);
        MemberAccount account = new MemberAccount();
        account.setId(1);
        account.setMember(member);
        account.setUsername(username);
        account.setPasswordHash("hash");
        account.setStatus("Active");
        return account;
    }

    private StaffAccount staffAccount(String username, String staffType) {
        User user = new User();
        Staff staff = new Staff();
        staff.setUser(user);
        staff.setStaffType(staffType);
        StaffAccount account = new StaffAccount();
        account.setId(1);
        account.setStaff(staff);
        account.setUsername(username);
        account.setPasswordHash("hash");
        account.setStatus("Active");
        return account;
    }
}
