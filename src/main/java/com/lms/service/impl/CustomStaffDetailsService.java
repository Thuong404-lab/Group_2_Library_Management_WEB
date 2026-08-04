package com.lms.service.impl;

import com.lms.config.CustomUserDetails;
import com.lms.entity.StaffAccount;
import com.lms.repository.StaffAccountRepository;
import com.lms.service.LocalizedMessageService;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

// check db xem tai khoan ton tai khong
@Service
public class CustomStaffDetailsService implements UserDetailsService {

    private final LocalizedMessageService messages;

    private final StaffAccountRepository staffAccountRepository;

    public CustomStaffDetailsService(StaffAccountRepository staffAccountRepository, LocalizedMessageService messages) {
        this.staffAccountRepository = staffAccountRepository;
        this.messages = messages;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        StaffAccount account = staffAccountRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        messages.get("backend.account.staffUsernameNotFound", username)));

        // Let CustomUserDetails handle the status validation (Inactive/Blocked)

        if (account.getStaff() == null || account.getStaff().getStaffType() == null) {
            throw new UsernameNotFoundException(
                    messages.get("backend.account.staffUsernameNotFound", username));
        }
        String authorityName;
        if ("Admin".equalsIgnoreCase(account.getStaff().getStaffType())) {
            authorityName = "ROLE_ADMIN";
        } else if ("Librarian".equalsIgnoreCase(account.getStaff().getStaffType())) {
            authorityName = "ROLE_LIBRARIAN";
        } else {
            throw new UsernameNotFoundException(
                    messages.get("backend.account.staffUsernameNotFound", username));
        }

        // Staff type, not a mutable join-table row, defines staff authority.
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(authorityName));

        return new CustomUserDetails(
                account.getStaff().getUser(),
                account.getUsername(),
                account.getPasswordHash(),
                account.getStatus(),
                account.getId(),
                false,
                authorities);
    }
}
