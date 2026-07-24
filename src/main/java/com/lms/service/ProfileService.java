package com.lms.service;

import com.lms.entity.User;
import org.springframework.web.multipart.MultipartFile;

/**
 * ProfileService - Xử lý Logic Hồ sơ (Member + Librarian)
 * Người phụ trách: Nguyễn Tiến Thương (CE191329)
 */
public interface ProfileService {

    // UC-4.1 & UC-16.1: Lấy thông tin Profile thông qua username/email đăng nhập
    User getProfile(String username);

    /** Resolve a staff profile without falling back to a member account. */
    User getStaffProfile(String username);

    long countActiveBorrows(String username);

    // UC-4.2 & UC-16.2: Cập nhật Profile
    void updateProfile(String username, String fullName, String email, String phone, MultipartFile avatarFile);

    void updateProfile(String currentUsername, String newUsername, String fullName, String email, String phone, MultipartFile avatarFile);

    /** Update staff-owned fields. Staff email and username are managed by an administrator. */
    void updateStaffProfile(String username, String fullName, String phone, MultipartFile avatarFile);

    // UC-4.3 + UC-16.3: Đổi mật khẩu
    void changePassword(String username, String oldPassword, String newPassword);

    void changeStaffPassword(String username, String oldPassword, String newPassword);
}
