package com.lms.service;

// =================================================================
// Huỳnh Gia Hưng bổ sung đoạn này: Import Entity MembershipTier
import com.lms.entity.MembershipTier;
// =================================================================

/**
 * MembershipService - Xử lý Logic Hạng Thành viên
 * Người phụ trách: Huỳnh Gia Hưng (CE190488)
 */
public interface MembershipService {

    // UC-5.1: Lấy quyền lợi theo Tier
    // =================================================================
    // Huỳnh Gia Hưng bổ sung đoạn này: Thay đổi kiểu trả về từ void sang MembershipTier để lấy dữ liệu
    MembershipTier getBenefits(Integer memberId);
    // =================================================================

    // UC-5.2: Lấy thông tin Tier hiện tại
    void getMembershipTier(Integer memberId);

}