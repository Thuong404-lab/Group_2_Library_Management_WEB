package com.lms.service;

import com.lms.dto.request.RegisterRequest;
import com.lms.entity.MemberAccount;

/**
 * AuthService Interface - Định nghĩa các method giao tiếp
 * Người phụ trách: Nguyễn Tiến Thương (CE191329)
 */
public interface AuthService {

    /**
     * Đăng ký thành viên mới
     */
    void register(RegisterRequest request) throws Exception;

    /**
     * Ghi log khi đăng nhập
     */
    void logLoginAction(Integer userId, String ipAddress, String userAgent, String sessionId);

    /**
     * Ghi log khi đăng xuất
     */
    void logLogoutAction(Integer userId, String ipAddress, String userAgent, String sessionId);

    /**
     * Tạo user, account, member, wallet cơ bản
     */
    MemberAccount createCoreAccount(String userName, String fullName, String pass, String email, String phone);

    /**
     * Yêu cầu đặt lại mật khẩu
     */
    void requestPasswordReset(String email) throws Exception;

    /**
     * Xác thực token đặt lại mật khẩu
     */
    void validatePasswordResetToken(String token) throws Exception;

    /**
     * Đặt lại mật khẩu cho người dùng
     */
    void resetPassword(String token, String newPassword) throws Exception;
}