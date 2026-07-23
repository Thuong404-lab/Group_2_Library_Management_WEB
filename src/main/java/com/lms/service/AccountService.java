package com.lms.service;

import com.lms.dto.request.AdminMemberAccountCreateRequest;
import com.lms.dto.request.AdminStaffAccountCreateRequest;
import com.lms.dto.request.AdminAccountUpdateRequest;
import com.lms.dto.response.AdminAccountListViewData;

import java.util.Map;

/**
 * AccountService - Xử lý business logic quản lý tài khoản của Admin.
 * Controller chỉ nhận request và trả view; Service chịu trách nhiệm validate,
 * thao tác entity/repository và ghi nhật ký hệ thống.
 *
 * Người phụ trách: Trần Ngọc Linh Đang (CE191088)
 */
public interface AccountService {

    // UC-20.4: Tìm kiếm / phân trang tài khoản thành viên
    AdminAccountListViewData getMemberAccountList(int page, String keyword, String status, String tier);

    // UC-20.1: Tạo tài khoản
    void createMemberAccount(AdminMemberAccountCreateRequest request);

    Map<String, String> validateMemberAccountCreate(AdminMemberAccountCreateRequest request);

    void createStaffAccount(AdminStaffAccountCreateRequest request);

    Map<String, String> validateStaffAccountCreate(AdminStaffAccountCreateRequest request);

    // UC-20.2: Cập nhật tài khoản
    void updateAccount(AdminAccountUpdateRequest request, Integer currentAccountId);

    // Validate realtime cho modal update
    Map<String, String> validateAccountUpdate(AdminAccountUpdateRequest request, Integer currentAccountId);

    // UC-20.3: Xóa tài khoản (soft delete)
    void deleteAccount(Integer accountId, String source, Integer currentAccountId);

    // Lấy email hiện tại của tài khoản thành viên để gửi liên kết đặt lại mật khẩu.
    String getMemberEmail(Integer accountId);

    // Lấy email hiện tại của tài khoản nhân sự để gửi liên kết đặt lại mật khẩu.
    String getStaffEmail(Integer accountId);
}
