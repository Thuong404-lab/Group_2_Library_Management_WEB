package com.lms.service;

import com.lms.entity.User;

/**
 * ProfileService - Xử lý Logic Hồ sơ (Member + Librarian)
 * Người phụ trách: Nguyễn Tiến Thương (CE191329)
 */
public interface ProfileService {

    // UC-4.1 & UC-16.1: Lấy thông tin Profile thông qua username/email đăng nhập
    User getProfile(String username);

    // UC-4.2 & UC-16.2: Cập nhật Profile
    void updateProfile(String username, String fullName, String email, String phone);

    // UC-4.3 + UC-16.3: Đổi mật khẩu
    void changePassword(String username, String oldPassword, String newPassword);
}