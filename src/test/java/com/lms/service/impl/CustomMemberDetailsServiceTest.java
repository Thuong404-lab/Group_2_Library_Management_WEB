package com.lms.service.impl;

import com.lms.entity.Member;
import com.lms.entity.MemberAccount;
import com.lms.entity.Role;
import com.lms.entity.User;
import com.lms.repository.MemberAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomMemberDetailsServiceTest {

    @Test
    void loadUserByUsernameUsesRoleMappedFromDatabase() {
        MemberAccountRepository repository = mock(MemberAccountRepository.class);
        MemberAccount account = new MemberAccount();
        Member member = mock(Member.class);
        User user = mock(User.class);

        account.setUsername("member01");
        account.setPasswordHash("encoded-password");
        account.setStatus("Active");
        account.setMember(member);
        account.setRoles(Set.of(new Role(3, "ROLE_MEMBER")));
        when(member.getUser()).thenReturn(user);
        when(repository.findByUsername("member01")).thenReturn(Optional.of(account));

        UserDetails userDetails = new CustomMemberDetailsService(repository)
                .loadUserByUsername("member01");

        assertThat(userDetails.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_MEMBER");
    }
}
