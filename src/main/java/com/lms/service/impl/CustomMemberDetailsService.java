package com.lms.service.impl;

import com.lms.config.CustomUserDetails;
import com.lms.entity.MemberAccount;
import com.lms.repository.MemberAccountRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class CustomMemberDetailsService implements UserDetailsService {

    private final MemberAccountRepository memberAccountRepository;

    public CustomMemberDetailsService(MemberAccountRepository memberAccountRepository) {
        this.memberAccountRepository = memberAccountRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        MemberAccount account = memberAccountRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản thành viên: " + username));

        if (!"Active".equalsIgnoreCase(account.getStatus())) {
            throw new org.springframework.security.authentication.DisabledException("Tài khoản đã bị khóa hoặc chưa kích hoạt.");
        }

        List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_MEMBER"));

        return new CustomUserDetails(
                account.getMember().getUser(),
                account.getUsername(),
                account.getPasswordHash(),
                account.getStatus(),
                account.getId(),
                authorities
        );
    }
}
