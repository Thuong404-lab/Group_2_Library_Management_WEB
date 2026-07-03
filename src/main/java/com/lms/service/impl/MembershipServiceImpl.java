package com.lms.service.impl;

import com.lms.entity.Member;
import com.lms.entity.MembershipTier;
import com.lms.repository.MemberRepository;
import com.lms.repository.MembershipTierRepository;
import com.lms.repository.MemberAccountRepository;
import com.lms.service.MembershipService;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class MembershipServiceImpl implements MembershipService {
    private final MemberRepository memberRepository;
    private final MembershipTierRepository membershipTierRepository;
    private final MemberAccountRepository memberAccountRepository;

    public MembershipServiceImpl(MemberRepository memberRepository,
                                 MembershipTierRepository membershipTierRepository,
                                 MemberAccountRepository memberAccountRepository) {
        this.memberRepository = memberRepository;
        this.membershipTierRepository = membershipTierRepository;
        this.memberAccountRepository = memberAccountRepository;
    }

    @Override
    public MembershipTier getBenefits(Integer memberId) {
        Optional<Member> memberOpt = memberRepository.findById(memberId);
        return memberOpt.map(Member::getTier).orElse(null);
    }

    @Override
    public Member getMembershipTier(Integer memberId) {
        Optional<Member> memberOpt = memberRepository.findById(memberId);
        return memberOpt.orElse(null);
    }

    @Override
    public Member getMemberByUsername(String username) {
        var account = memberAccountRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Account not found for: " + username));

        return account.getMember();
    }

    // --- BỔ SUNG XỬ LÝ LOGIC TIẾN TRÌNH HẠNG ĐỘNG CHO VIEW ---

    @Override
    public List<MembershipTier> getAllTiers() {
        return membershipTierRepository.findAll().stream()
                .sorted((t1, t2) -> {
                    if (t1.getCondition() == null) return -1;
                    if (t2.getCondition() == null) return 1;
                    return t1.getCondition().compareTo(t2.getCondition());
                })
                .toList();
    }

    @Override
    public double getAccumulatedSpending(Member member) {
        return 150000.0;
    }

    @Override
    public MembershipTier getNextTier(MembershipTier currentTier) {
        if (currentTier == null) return null;
        List<MembershipTier> tiers = getAllTiers();

        return tiers.stream()
                .filter(t -> t.getCondition() != null && currentTier.getCondition() != null
                        && t.getCondition().compareTo(currentTier.getCondition()) > 0)
                .findFirst()
                .orElse(null);
    }
    @Override
    public List<Member> getTopMembersBySpending() {
        return memberRepository.findAll().stream()
                .limit(5)
                .toList();
    }
}