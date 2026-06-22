package com.lms.service;

/**
 * MembershipService - Xử lý Logic Hạng Thành viên
 * Người phụ trách: Huỳnh Gia Hưng (CE190488)
 */
public interface MembershipService {

    // UC-5.1: Lấy quyền lợi theo Tier
    void getBenefits(Integer memberId);

    // UC-5.2: Lấy thông tin Tier hiện tại
    void getMembershipTier(Integer memberId);

}
