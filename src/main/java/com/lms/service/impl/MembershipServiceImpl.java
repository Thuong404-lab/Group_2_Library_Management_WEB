package com.lms.service.impl;

import com.lms.entity.Member;
import com.lms.entity.MembershipTier;
import com.lms.exception.ConflictException;
import com.lms.exception.ResourceNotFoundException;
import com.lms.exception.ValidationException;
import com.lms.repository.MemberAccountRepository;
import com.lms.repository.MemberRepository;
import com.lms.repository.MembershipTierRepository;
import com.lms.repository.TransactionRepository;
import com.lms.service.LocalizedMessageService;
import com.lms.service.MembershipService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.function.Function;

@Service
public class MembershipServiceImpl implements MembershipService {

    private final LocalizedMessageService messages;
    private final MemberRepository memberRepository;
    private final MembershipTierRepository membershipTierRepository;
    private final MemberAccountRepository memberAccountRepository;
    private final TransactionRepository transactionRepository;

    public MembershipServiceImpl(LocalizedMessageService messages,
                                  MemberRepository memberRepository,
                                  MembershipTierRepository membershipTierRepository,
                                  MemberAccountRepository memberAccountRepository,
                                  TransactionRepository transactionRepository) {
        this.messages = messages;
        this.memberRepository = memberRepository;
        this.membershipTierRepository = membershipTierRepository;
        this.memberAccountRepository = memberAccountRepository;
        this.transactionRepository = transactionRepository;
    }

    // ── Public read methods ────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public MembershipTier getBenefits(Integer memberId) {
        Optional<Member> memberOpt = memberRepository.findById(memberId);
        return memberOpt.map(Member::getTier).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public Member getMembershipTier(Integer memberId) {
        return memberRepository.findById(memberId).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public Member getMemberByUsername(String username) {
        var account = memberAccountRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messages.get("backend.profile.accountNotFound", username)));
        return account.getMember();
    }

    // ── Tier progress (member-facing) ──────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<MembershipTier> getAllTiers() {
        // Sort thực hiện ở DB, không sort lại ở memory
        return membershipTierRepository.findAllByOrderByConditionAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public double getAccumulatedSpending(Member member) {
        // Only completed top-ups contribute to the configured membership threshold.
        if (member == null || member.getMemberId() == null) {
            return 0.0;
        }
        BigDecimal total = transactionRepository.sumCompletedTopUpByMemberId(member.getMemberId());
        return total == null ? 0.0 : total.doubleValue();
    }

    @Override
    @Transactional(readOnly = true)
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
    @Transactional(readOnly = true)
    public List<Member> getTopMembersBySpending() {
        // Rank in the database so the result is deterministic and does not load every member.
        List<Integer> memberIds = transactionRepository.findTopMembersByCompletedTopUp(PageRequest.of(0, 5))
                .stream()
                .map(row -> (Integer) row[0])
                .toList();
        Map<Integer, Member> membersById = memberRepository.findAllById(memberIds).stream()
                .collect(Collectors.toMap(Member::getMemberId, Function.identity()));
        return memberIds.stream().map(membersById::get).filter(java.util.Objects::nonNull).toList();
    }

    // ── UC-22.3: Membership Tier Management (Admin) ────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public MembershipTier getTierById(Integer id) {
        return membershipTierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(messages.get("backend.tier.notFound")));
    }

    @Override
    @Transactional
    public void saveTier(MembershipTier tier) {
        // 1. Tên hạng
        if (tier.getTierName() == null || tier.getTierName().isBlank()) {
            throw new ValidationException(messages.get("backend.tier.nameRequired"));
        }
        tier.setTierName(tier.getTierName().trim());

        // 2. Không trùng tên (case-insensitive)
        boolean nameTaken = (tier.getTierId() == null)
                ? membershipTierRepository.existsByTierNameIgnoreCase(tier.getTierName())
                : membershipTierRepository.existsByTierNameIgnoreCaseAndTierIdNot(
                        tier.getTierName(), tier.getTierId());
        if (nameTaken) {
            throw new ValidationException(messages.get("backend.tier.nameExists"));
        }

        // 3. Phần trăm giảm giá
        if (tier.getDiscountPercent() == null) {
            throw new ValidationException(messages.get("backend.tier.discountRequired"));
        }
        if (tier.getDiscountPercent().compareTo(BigDecimal.ZERO) < 0
                || tier.getDiscountPercent().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new ValidationException(messages.get("backend.tier.discountRange"));
        }

        // 4. Giới hạn mượn (phải >= 1)
        if (tier.getBorrowLimit() == null || tier.getBorrowLimit() < 1) {
            throw new ValidationException(messages.get("backend.tier.borrowLimitMin"));
        }

        // 5. Điều kiện chi tiêu
        if (tier.getCondition() == null) {
            throw new ValidationException(messages.get("backend.tier.conditionRequired"));
        }
        if (tier.getCondition().compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException(messages.get("backend.tier.conditionNonNegative"));
        }

        membershipTierRepository.save(tier);
    }

    @Override
    @Transactional
    public void deleteTier(Integer id) {
        MembershipTier tier = getTierById(id);
        // Dùng existsByTier_TierId thay vì findAll().stream() — O(1), không load toàn bộ bảng
        if (memberRepository.existsByTier_TierId(id)) {
            throw new ConflictException(messages.get("backend.tier.deleteInUse"));
        }
        membershipTierRepository.delete(tier);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Integer, Long> getMemberCountByTier() {
        // Một query GROUP BY duy nhất thay vì N query cho N tiers
        return memberRepository.countGroupByTierId().stream()
                .collect(Collectors.toMap(
                        row -> (Integer) row[0],
                        row -> (Long)    row[1]
                ));
    }
}
