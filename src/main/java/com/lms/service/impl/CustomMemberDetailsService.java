package com.lms.service.impl;

import com.lms.config.CustomUserDetails;
import com.lms.entity.MemberAccount;
import com.lms.repository.MemberAccountRepository;
import com.lms.service.LocalizedMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

// check db xem tai khoan ton tai khong
@Service
public class CustomMemberDetailsService implements UserDetailsService {

    @Autowired
    private LocalizedMessageService messages = LocalizedMessageService.fallback();

    private final MemberAccountRepository memberAccountRepository;

    public CustomMemberDetailsService(MemberAccountRepository memberAccountRepository) {
        this.memberAccountRepository = memberAccountRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        MemberAccount account = memberAccountRepository.findByUsername(username)
                .orElseGet(() -> memberAccountRepository.findByMember_User_Email(username)
                        .orElseThrow(() -> new UsernameNotFoundException(
                                messages.get("backend.account.memberUsernameNotFound", username))));

        // Let CustomUserDetails handle the status validation (Inactive/Blocked)

        List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_MEMBER"));

        return new CustomUserDetails(
                account.getMember().getUser(),
                account.getUsername(),
                account.getPasswordHash(),
                account.getStatus(),
                account.getId(),
                authorities);
    }
}
