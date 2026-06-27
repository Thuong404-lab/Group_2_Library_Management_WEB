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
import com.lms.repository.AccountRepository;
import org.springframework.stereotype.Service;

/**
 * MembershipService - Xử lý Logic Hạng Thành viên
 * Người phụ trách: Huỳnh Gia Hưng (CE190488)
 */
@Service
public class MembershipServiceImpl implements MembershipService {
    private final MemberRepository memberRepository;
    private final MembershipTierRepository membershipTierRepository;
    private final AccountRepository accountRepository;

    public MembershipServiceImpl(MemberRepository memberRepository, MembershipTierRepository membershipTierRepository, AccountRepository accountRepository) {
        this.memberRepository = memberRepository;
        this.membershipTierRepository = membershipTierRepository;
        this.accountRepository = accountRepository;
    }

    // UC-5.1: Lấy quyền lợi theo Tier (GIỮ NGUYÊN)
    @Override
    public MembershipTier getBenefits(Integer memberId) {
        Optional<Member> memberOpt = memberRepository.findById(memberId);
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
        Optional<Member> memberOpt = memberRepository.findById(memberId);
        return memberOpt.orElse(null);
    }
    // =================================================================

    // Lấy Member bằng username từ account đang đăng nhập
    @Override
    public Member getMemberByUsername(String username) {
        var account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Account not found for: " + username));
        
        var user = account.getUser();
        if (user == null) {
            throw new RuntimeException("User profile not linked to account: " + username);
        }

        var memberOpt = memberRepository.findByUserEmail(user.getEmail());
        return memberOpt.orElse(null);
    }
}