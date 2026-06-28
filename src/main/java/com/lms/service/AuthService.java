package com.lms.service;

import com.lms.dto.request.RegisterRequest;
import com.lms.entity.Account;

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

    Account createCoreAccount(String userName, String fullName, String pass, String email, String phone);
}
