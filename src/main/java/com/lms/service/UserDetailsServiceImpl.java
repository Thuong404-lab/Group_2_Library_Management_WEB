package com.lms.service;

import com.lms.config.CustomUserDetails;
import com.lms.entity.Account;
import com.lms.entity.Staff;
import com.lms.repository.AccountRepository;
import com.lms.repository.MemberRepository;
import com.lms.repository.StaffRepository;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private AccountRepository accountRepository;
    
    @Autowired
    private StaffRepository staffRepository;
    
    @Autowired
    private MemberRepository memberRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản: " + username));
        
        List<GrantedAuthority> authorities = new ArrayList<>();
        
        // Xác định Role
        Optional<Staff> staffOpt = staffRepository.findByUserUserId(account.getUser().getUserId());
        if (staffOpt.isPresent()) {
            String type = staffOpt.get().getStaffType().toUpperCase(); // ADMIN hoặc LIBRARIAN
            authorities.add(new SimpleGrantedAuthority("ROLE_" + type));
        } else {
            // Nếu không phải Staff thì là Member
            authorities.add(new SimpleGrantedAuthority("ROLE_MEMBER"));
        }
        
        return new CustomUserDetails(account, authorities);
    }
}
