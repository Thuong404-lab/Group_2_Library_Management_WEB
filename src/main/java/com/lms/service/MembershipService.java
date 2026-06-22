package com.lms.service;

import org.springframework.stereotype.Service;

/**
 * MembershipService - Xử lý Logic Hạng Thành viên
 * Người phụ trách: Huỳnh Gia Hưng (CE190488)
 */
@Service
public class MembershipService {

    // UC-5.1: Lấy quyền lợi theo Tier
    public void getBenefits(Integer memberId) {
        // TODO: Implement - Trả về danh sách quyền lợi theo MembershipTier
    }

    // UC-5.2: Lấy thông tin Tier hiện tại
    public void getMembershipTier(Integer memberId) {
        // TODO: Implement - Trả về Tier hiện tại + điểm tích lũy
    }
}
