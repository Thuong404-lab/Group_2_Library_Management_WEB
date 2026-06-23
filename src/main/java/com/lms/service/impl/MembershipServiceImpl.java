package com.lms.service.impl;

// =================================================================
// Huỳnh Gia Hưng bổ sung đoạn này: Import Entity cần thiết
import com.lms.entity.Member;
import com.lms.entity.MembershipTier;
import java.util.Optional;
// =================================================================

import com.lms.service.MembershipService;
import com.lms.repository.MemberRepository;
import com.lms.repository.MembershipTierRepository;
import org.springframework.stereotype.Service;

/**
 * MembershipService - Xử lý Logic Hạng Thành viên
 * Người phụ trách: Huỳnh Gia Hưng (CE190488)
 */
@Service
public class MembershipServiceImpl implements MembershipService {
    private final MemberRepository memberRepository;
    private final MembershipTierRepository membershipTierRepository;

    public MembershipServiceImpl(MemberRepository memberRepository, MembershipTierRepository membershipTierRepository) {
        this.memberRepository = memberRepository;
        this.membershipTierRepository = membershipTierRepository;
    }


    // UC-5.1: Lấy quyền lợi theo Tier
    @Override
    // =================================================================
    // Huỳnh Gia Hưng bổ sung đoạn này: Sửa void thành MembershipTier và triển khai logic
    public MembershipTier getBenefits(Integer memberId) {
        // Tận dụng hàm findByUserId có sẵn trong MemberRepository của bạn
        Optional<Member> memberOpt = memberRepository.findByUserId(memberId);

        if (memberOpt.isPresent()) {
            return memberOpt.get().getTier(); // Trả về đối tượng MembershipTier gắn với Member này
        }
        return null;
    }
    // =================================================================

    // UC-5.2: Lấy thông tin Tier hiện tại
    @Override
    public void getMembershipTier(Integer memberId) {
        // TODO: Implement - Trả về Tier hiện tại + điểm tích lũy
    }
}