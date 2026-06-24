package com.lms.service.impl;

// =================================================================
// Huỳnh Gia Hưng bổ sung đoạn này: Import Entity
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

    // UC-5.1: Lấy quyền lợi theo Tier (GIỮ NGUYÊN)
    @Override
    public MembershipTier getBenefits(Integer memberId) {
        Optional<Member> memberOpt = memberRepository.findByUserId(memberId);
        if (memberOpt.isPresent()) {
            return memberOpt.get().getTier();
        }
        return null;
    }

    // UC-5.2: Lấy thông tin Tier hiện tại (BỔ SUNG THÊM)
    @Override
    // =================================================================
    // Huỳnh Gia Hưng bổ sung đoạn này: Logic lấy thông tin Member cho UC-5.2
    public Member getMembershipTier(Integer memberId) {
        Optional<Member> memberOpt = memberRepository.findByUserId(memberId);
        return memberOpt.orElse(null);
    }
    // =================================================================
}