package com.lms.service.impl;

import com.lms.service.ProfileService;

import com.lms.repository.UserRepository;
import com.lms.repository.AccountRepository;
import org.springframework.stereotype.Service;

/**
 * ProfileService - Xử lý Logic Hồ sơ (Member + Librarian)
 * Người phụ trách: Nguyễn Tiến Thương (CE191329)
 */
@Service
public class ProfileServiceImpl implements ProfileService {
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;

    public ProfileServiceImpl(UserRepository userRepository, AccountRepository accountRepository) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
    }


    // UC-4.1: Lấy thông tin Profile
    @Override
    public void getProfile(String username) {
        // TODO: Implement
    }

    // UC-4.2: Cập nhật Profile
    @Override
    public void updateProfile(Integer memberId, String fullName, String email, String phone) {
        // TODO: Implement
    }

    // UC-4.3 + UC-16.3: Đổi mật khẩu
    @Override
    public void changePassword(String username, String oldPassword, String newPassword) {
        // TODO: Implement - Validate mật khẩu cũ, mã hóa mật khẩu mới
    }
}
