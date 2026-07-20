package com.lms.service;

import com.lms.entity.MembershipTier;
import com.lms.entity.Member;

import java.util.List;
import java.util.Map;

public interface MembershipService {
    MembershipTier getBenefits(Integer memberId);
    Member getMembershipTier(Integer memberId);
    Member getMemberByUsername(String username);

    // Phục vụ tính toán tiến trình nâng hạng trên giao diện HTML
    List<MembershipTier> getAllTiers();
    double getAccumulatedSpending(Member member);
    MembershipTier getNextTier(MembershipTier currentTier);

    List<Member> getTopMembersBySpending();

    // UC-22.3: Membership Tier Management (Admin)
    MembershipTier getTierById(Integer id);
    void saveTier(MembershipTier tier);
    void deleteTier(Integer id);

    /** Trả về Map<tierId, memberCount> — dùng để hiển thị số thành viên trong bảng admin */
    Map<Integer, Long> getMemberCountByTier();
}
