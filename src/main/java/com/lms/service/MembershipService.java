package com.lms.service;

import com.lms.entity.MembershipTier;
import com.lms.entity.Member;
import com.lms.dto.request.MembershipTierUpdateRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface MembershipService {
    MembershipTier getBenefits(Integer memberId);
    Member getMembershipTier(Integer memberId);
    Member getMemberByUsername(String username);

    // Phục vụ tính toán tiến trình nâng hạng trên giao diện HTML
    List<MembershipTier> getAllTiers();
    BigDecimal getAccumulatedSpending(Member member);
    MembershipTier getNextTier(MembershipTier currentTier);

    List<Member> getTopMembersBySpending();

    // UC-22.3: Membership Tier Management (Admin)
    MembershipTier getTierById(Integer id);
    int updateTier(MembershipTierUpdateRequest request);
    void deleteTier(Integer id);
    void synchronizeMemberTier(Integer memberId);
    int synchronizeAllMemberTiers();

    /** Trả về Map<tierId, memberCount> — dùng để hiển thị số thành viên trong bảng admin */
    Map<Integer, Long> getMemberCountByTier();
}
