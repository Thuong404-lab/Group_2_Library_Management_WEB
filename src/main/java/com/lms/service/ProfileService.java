package com.lms.service;

import org.springframework.stereotype.Service;

/**
 * ProfileService - Xử lý Logic Hồ sơ (Member + Librarian)
 * Người phụ trách: Nguyễn Tiến Thương (CE191329)
 */
@Service
public class ProfileService {

    // UC-4.1: Lấy thông tin Profile
    public void getProfile(String username) {
        // TODO: Implement
    }

    // UC-4.2: Cập nhật Profile
    public void updateProfile(Integer memberId, String fullName, String email, String phone) {
        // TODO: Implement
    }

    // UC-4.3 + UC-16.3: Đổi mật khẩu
    public void changePassword(String username, String oldPassword, String newPassword) {
        // TODO: Implement - Validate mật khẩu cũ, mã hóa mật khẩu mới
    }
}
