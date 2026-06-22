package com.lms.service.impl;

import com.lms.service.ProfileService;

import org.springframework.stereotype.Service;

/**
 * ProfileService - Xử lý Logic Hồ sơ (Member + Librarian)
 * Người phụ trách: Nguyễn Tiến Thương (CE191329)
 */
@Service
public class ProfileServiceImpl implements ProfileService {

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
