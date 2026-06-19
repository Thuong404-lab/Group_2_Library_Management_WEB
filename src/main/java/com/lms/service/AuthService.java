package com.lms.service;

import org.springframework.stereotype.Service;

/**
 * AuthService - Xử lý Logic Xác thực
 * Người phụ trách: Nguyễn Tiến Thương (CE191329)
 */
@Service
public class AuthService {

    // UC-9: Xác thực đăng nhập
    public void authenticate(String username, String password) {
        // TODO: Implement - Spring Security sẽ xử lý qua UserDetailsService
    }

    // UC-2: Đăng ký thành viên mới
    public void register(String username, String password, String fullName, String email, String phone) {
        // TODO: Implement - Tạo Account + Member + Wallet
        // TODO: Mã hóa password bằng BCryptPasswordEncoder
        // TODO: Gán MembershipTier mặc định (Bronze)
    }
}
