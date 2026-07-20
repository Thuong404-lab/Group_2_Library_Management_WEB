package com.lms.service.impl;

import com.lms.entity.Role;
import com.lms.entity.Staff;
import com.lms.entity.StaffAccount;
import com.lms.entity.User;
import com.lms.repository.StaffAccountRepository;
import com.lms.service.LocalizedMessageService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomStaffDetailsServiceTest {

    @ParameterizedTest
    @ValueSource(strings = {"ROLE_ADMIN", "ROLE_LIBRARIAN"})
    void loadUserByUsernamePreservesCanonicalRoleName(String roleName) {
        StaffAccountRepository repository = mock(StaffAccountRepository.class);
        LocalizedMessageService messages = mock(LocalizedMessageService.class);
        StaffAccount account = new StaffAccount();
        Staff staff = mock(Staff.class);
        User user = mock(User.class);

        account.setUsername("staff01");
        account.setPasswordHash("encoded-password");
        account.setStatus("Active");
        account.setStaff(staff);
        account.setRoles(Set.of(new Role(1, roleName)));
        when(staff.getUser()).thenReturn(user);
        when(repository.findByUsername("staff01")).thenReturn(Optional.of(account));

        UserDetails userDetails = new CustomStaffDetailsService(repository, messages)
                .loadUserByUsername("staff01");

        assertThat(userDetails.getAuthorities())
                .extracting("authority")
                .containsExactly(roleName);
    }
}
