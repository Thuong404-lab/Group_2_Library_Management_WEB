package com.lms.service;

import com.lms.entity.MembershipTier;
import com.lms.entity.Member;
import java.util.List;

public interface MembershipService {
    MembershipTier getBenefits(Integer memberId);
    Member getMembershipTier(Integer memberId);
    Member getMemberByUsername(String username);

    // BỔ SUNG: Phục vụ tính toán tiến trình nâng hạng trên giao diện HTML mới
    List<MembershipTier> getAllTiers();
    double getAccumulatedSpending(Member member);
    MembershipTier getNextTier(MembershipTier currentTier);

    List<Member> getTopMembersBySpending(); // Lấy danh sách thành viên chi tiêu nhiều nhất làm bảng xếp hạng
}