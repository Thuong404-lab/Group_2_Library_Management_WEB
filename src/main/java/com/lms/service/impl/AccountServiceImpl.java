package com.lms.service.impl;

import com.lms.service.AccountService;

import org.springframework.stereotype.Service;

/**
 * AccountService - Xử lý Logic Quản lý Tài khoản (Admin)
 * Người phụ trách: Trần Ngọc Linh Đang (CE191088)
 */
@Service
public class AccountServiceImpl implements AccountService {

    // UC-20.1: Tạo tài khoản
    @Override
    public void createAccount(String username, String password, String role) {
        // TODO: Implement
    }

    // UC-20.2: Cập nhật tài khoản
    @Override
    public void updateAccount(Integer accountId, String email, String role) {
        // TODO: Implement
    }

    // UC-20.3: Xóa tài khoản (soft delete)
    @Override
    public void deleteAccount(Integer accountId) {
        // TODO: Implement
    }

    // UC-20.4: Tìm kiếm tài khoản
    @Override
    public void searchAccounts(String keyword) {
        // TODO: Implement
    }

    // UC-20.5: Đổi trạng thái tài khoản
    @Override
    public void changeAccountStatus(Integer accountId, String status) {
        // TODO: Implement
    }

    // UC-20.6: Reset password
    @Override
    public void resetPassword(Integer accountId) {
        // TODO: Implement - Reset về mật khẩu mặc định
    }
}
