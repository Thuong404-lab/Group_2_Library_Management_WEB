package com.lms.service;

/**
 * AccountService - Xử lý Logic Quản lý Tài khoản (Admin)
 * Người phụ trách: Trần Ngọc Linh Đang (CE191088)
 */
public interface AccountService {

    // UC-20.1: Tạo tài khoản
    void createAccount(String username, String password, String role);

    // UC-20.2: Cập nhật tài khoản
    void updateAccount(Integer accountId, String email, String role);

    // UC-20.3: Xóa tài khoản (soft delete)
    void deleteAccount(Integer accountId);

    // UC-20.4: Tìm kiếm tài khoản
    void searchAccounts(String keyword);

    // UC-20.5: Đổi trạng thái tài khoản
    void changeAccountStatus(Integer accountId, String status);

    // UC-20.6: Reset password
    void resetPassword(Integer accountId);

}
