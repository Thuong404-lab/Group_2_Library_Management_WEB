package com.lms.service;

import com.lms.dto.request.AdminAccountCreateRequest;
import com.lms.dto.request.AdminAccountUpdateRequest;
import com.lms.entity.Member;
import com.lms.entity.MemberAccount;
import com.lms.entity.MembershipTier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
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
    Page<MemberAccount> getMemberAccounts(String keyword, Pageable pageable);

    // Dữ liệu phụ trợ cho màn hình admin account
    Map<Integer, Member> getMemberByUserId(Page<MemberAccount> accounts);

    List<MembershipTier> getMembershipTiers();

    // UC-20.1: Tạo tài khoản
    void createAccount(AdminAccountCreateRequest request);

    // UC-20.2: Cập nhật tài khoản
    void updateAccount(AdminAccountUpdateRequest request);

    // Validate realtime cho modal update
    Map<String, String> validateAccountUpdate(AdminAccountUpdateRequest request);

    // UC-20.3: Xóa tài khoản (soft delete)
    void deleteAccount(Integer accountId, String source);

    // Lấy email hiện tại của tài khoản thành viên để gửi liên kết đặt lại mật khẩu.
    String getMemberEmail(Integer accountId);
}
