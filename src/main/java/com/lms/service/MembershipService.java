package com.lms.service;

// =================================================================
// Huỳnh Gia Hưng bổ sung đoạn này: Import Entity
import com.lms.entity.MembershipTier;
import com.lms.entity.Member;
// =================================================================

/**
 * MembershipService - Xử lý Logic Hạng Thành viên
 * Người phụ trách: Huỳnh Gia Hưng (CE190488)
 */
public interface MembershipService {

    // UC-5.1: Lấy quyền lợi theo Tier
    MembershipTier getBenefits(Integer memberId);

    // UC-5.2: Lấy thông tin Tier hiện tại
    // =================================================================
    // Huỳnh Gia Hưng bổ sung đoạn này: Đổi void sang Member cho UC-5.2
    Member getMembershipTier(Integer memberId);
    // =================================================================

}