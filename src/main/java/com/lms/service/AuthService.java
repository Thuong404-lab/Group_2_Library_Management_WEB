package com.lms.service;

import com.lms.dto.request.RegisterRequest;

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
    void logLoginAction(Integer accountId, String ipAddress, String userAgent, String sessionId);

    /**
     * Ghi log khi đăng xuất
     */
    void logLogoutAction(Integer accountId, String ipAddress, String userAgent, String sessionId);

    /**
     * Yêu cầu đặt lại mật khẩu
     */
    void requestPasswordReset(String email) throws Exception;

    /**
     * Xác thực token đặt lại mật khẩu
     * @param token Token đặt lại mật khẩu
     * @throws Exception Nếu token không hợp lệ hoặc đã hết hạn
     */
    void validatePasswordResetToken(String token) throws Exception;

    /**
     * Đặt lại mật khẩu cho người dùng
     * @param token Token đặt lại mật khẩu
     * @param newPassword Mật khẩu mới
     * @throws Exception Nếu token không hợp lệ hoặc có lỗi khi cập nhật mật khẩu
     */
    void resetPassword(String token, String newPassword) throws Exception;
}
