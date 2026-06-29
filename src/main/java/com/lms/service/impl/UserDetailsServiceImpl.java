package com.lms.service.impl;

import com.lms.config.CustomUserDetails;
import com.lms.entity.Account;
import com.lms.entity.Staff;
import com.lms.repository.AccountRepository;
import com.lms.repository.MemberRepository;
import com.lms.repository.StaffRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * UserDetailsServiceImpl - Load thông tin User từ Database cho Spring Security
 * Người phụ trách: Nguyễn Tiến Thương (CE191329)
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final AccountRepository accountRepository;

    private final StaffRepository staffRepository;

    private final MemberRepository memberRepository;

    public UserDetailsServiceImpl(AccountRepository accountRepository, StaffRepository staffRepository,
            MemberRepository memberRepository) {
        this.accountRepository = accountRepository;
        this.staffRepository = staffRepository;
        this.memberRepository = memberRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        Account account;
        if (usernameOrEmail.contains("@")) {
            account = accountRepository.findByUser_Email(usernameOrEmail)
                    .orElseThrow(() -> new UsernameNotFoundException(
                            "Không tìm thấy tài khoản với email: " + usernameOrEmail));
        } else {
            account = accountRepository.findByUsername(usernameOrEmail)
                    .orElseThrow(() -> new UsernameNotFoundException(
                            "Không tìm thấy tài khoản với username: " + usernameOrEmail));
        }

        List<GrantedAuthority> authorities = new ArrayList<>();

        Optional<Staff> staffOpt = staffRepository.findByUserId(account.getUser().getId());
        if (staffOpt.isPresent()) {
            String type = staffOpt.get().getStaffType().toUpperCase();
            authorities.add(new SimpleGrantedAuthority("ROLE_" + type));
        } else {
            authorities.add(new SimpleGrantedAuthority("ROLE_MEMBER"));
        }

        return new CustomUserDetails(account, authorities);
    }
}
