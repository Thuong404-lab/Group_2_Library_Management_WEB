package com.lms.service;

/**
 * ProfileService - Xử lý Logic Hồ sơ (Member + Librarian)
 * Người phụ trách: Nguyễn Tiến Thương (CE191329)
 */
public interface ProfileService {

    // UC-4.1: Lấy thông tin Profile
    void getProfile(String username);

    // UC-4.2: Cập nhật Profile
    void updateProfile(Integer memberId, String fullName, String email, String phone);

    // UC-4.3 + UC-16.3: Đổi mật khẩu
    void changePassword(String username, String oldPassword, String newPassword);

}
