package com.lms.service.impl;

import com.lms.config.CustomUserDetails;
import com.lms.entity.StaffAccount;
import com.lms.repository.StaffAccountRepository;
import com.lms.service.LocalizedMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;
import java.util.List;

// check db xem tai khoan ton tai khong
@Service
public class CustomStaffDetailsService implements UserDetailsService {

    @Autowired
    private LocalizedMessageService messages = LocalizedMessageService.fallback();

    private final StaffAccountRepository staffAccountRepository;

    public CustomStaffDetailsService(StaffAccountRepository staffAccountRepository) {
        this.staffAccountRepository = staffAccountRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        StaffAccount account = staffAccountRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(messages.get("backend.account.staffUsernameNotFound", username)));

        if (!"Active".equalsIgnoreCase(account.getStatus())) {
            throw new DisabledException(messages.get("backend.account.disabled"));
        }

        List<GrantedAuthority> authorities = account.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName().toUpperCase()))
                .collect(Collectors.toList());

        return new CustomUserDetails(
                account.getStaff().getUser(),
                account.getUsername(),
                account.getPasswordHash(),
                account.getStatus(),
                account.getId(),
                authorities
        );
    }
}
