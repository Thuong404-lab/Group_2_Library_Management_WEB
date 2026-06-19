package com.lms.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * UserDetailsServiceImpl - Load thông tin User từ Database cho Spring Security
 * Người phụ trách: Nguyễn Tiến Thương (CE191329)
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // TODO: Implement - Tìm Account theo username trong AccountRepository
        // TODO: Nếu không tìm thấy → throw UsernameNotFoundException
        // TODO: Xác định role (Admin/Librarian/Member) dựa vào Staff hoặc Member table
        // TODO: Trả về CustomUserDetails chứa thông tin đăng nhập
        throw new UsernameNotFoundException("User not found: " + username);
    }
}
