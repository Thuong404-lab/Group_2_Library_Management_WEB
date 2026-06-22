package com.lms.service.impl;

import com.lms.service.MembershipService;

import org.springframework.stereotype.Service;

/**
 * MembershipService - Xử lý Logic Hạng Thành viên
 * Người phụ trách: Huỳnh Gia Hưng (CE190488)
 */
@Service
public class MembershipServiceImpl implements MembershipService {

    // UC-5.1: Lấy quyền lợi theo Tier
    @Override
    public void getBenefits(Integer memberId) {
        // TODO: Implement - Trả về danh sách quyền lợi theo MembershipTier
    }

    // UC-5.2: Lấy thông tin Tier hiện tại
    @Override
    public void getMembershipTier(Integer memberId) {
        // TODO: Implement - Trả về Tier hiện tại + điểm tích lũy
    }
}
