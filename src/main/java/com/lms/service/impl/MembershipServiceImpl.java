package com.lms.service.impl;

import com.lms.entity.Member;
import com.lms.entity.MembershipTier;
import com.lms.dto.request.MembershipTierUpdateRequest;
import com.lms.enums.ActionType;
import com.lms.exception.ConflictException;
import com.lms.exception.ResourceNotFoundException;
import com.lms.exception.ValidationException;
import com.lms.repository.MemberAccountRepository;
import com.lms.repository.MemberRepository;
import com.lms.repository.MembershipTierRepository;
import com.lms.repository.MembershipTierTranslationRepository;
import com.lms.repository.TransactionRepository;
import com.lms.service.LocalizedMessageService;
import com.lms.service.MembershipService;
import com.lms.service.AuditLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.function.Function;
import org.springframework.context.i18n.LocaleContextHolder;

@Service
public class MembershipServiceImpl implements MembershipService {

    private final LocalizedMessageService messages;
    private final MemberRepository memberRepository;
    private final MembershipTierRepository membershipTierRepository;
    private final MembershipTierTranslationRepository membershipTierTranslationRepository;
    private final MemberAccountRepository memberAccountRepository;
    private final TransactionRepository transactionRepository;
    private final AuditLogService auditLogService;

    public MembershipServiceImpl(LocalizedMessageService messages,
                                  MemberRepository memberRepository,
                                  MembershipTierRepository membershipTierRepository,
                                  MembershipTierTranslationRepository membershipTierTranslationRepository,
                                  MemberAccountRepository memberAccountRepository,
                                  TransactionRepository transactionRepository,
                                  AuditLogService auditLogService) {
        this.messages = messages;
        this.memberRepository = memberRepository;
        this.membershipTierRepository = membershipTierRepository;
        this.membershipTierTranslationRepository = membershipTierTranslationRepository;
        this.memberAccountRepository = memberAccountRepository;
        this.transactionRepository = transactionRepository;
        this.auditLogService = auditLogService;
    }

    // ── Public read methods ────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public MembershipTier getBenefits(Integer memberId) {
        Optional<Member> memberOpt = memberRepository.findById(memberId);
        return memberOpt.map(Member::getTier).map(this::localizeTier).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public Member getMembershipTier(Integer memberId) {
        Member member = memberRepository.findById(memberId).orElse(null);
        if (member != null && member.getTier() != null) localizeTier(member.getTier());
        return member;
    }

    @Override
    @Transactional(readOnly = true)
    public Member getMemberByUsername(String username) {
        var account = memberAccountRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messages.get("backend.profile.accountNotFound", username)));
        Member member = account.getMember();
        if (member != null && member.getTier() != null) localizeTier(member.getTier());
        return member;
    }

    // ── Tier progress (member-facing) ──────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<MembershipTier> getAllTiers() {
        List<MembershipTier> tiers = membershipTierRepository.findAllByOrderByConditionAsc();
        String language = LocaleContextHolder.getLocale().getLanguage();
        membershipTierTranslationRepository.findByLanguageCode(language).forEach(translation -> {
            MembershipTier tier = translation.getTier();
            tier.setDisplayName(translation.getTierName());
            tier.setDisplayBenefits(translation.getBenefits());
        });
        return tiers;
    }

    private MembershipTier localizeTier(MembershipTier tier) {
        if (tier == null || tier.getTierId() == null) return tier;
        String language = LocaleContextHolder.getLocale().getLanguage();
        membershipTierTranslationRepository.findByTierTierIdAndLanguageCode(tier.getTierId(), language)
                .ifPresent(translation -> {
                    tier.setDisplayName(translation.getTierName());
                    tier.setDisplayBenefits(translation.getBenefits());
                });
        return tier;
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getAccumulatedSpending(Member member) {
        // Loyalty spending counts paid library services, never wallet top-ups or penalties.
        if (member == null || member.getMemberId() == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = transactionRepository.sumCompletedMembershipSpendByMemberId(member.getMemberId());
        return total == null ? BigDecimal.ZERO : total;
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
        List<Integer> memberIds = transactionRepository.findTopMembersByCompletedMembershipSpend(PageRequest.of(0, 5))
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
    public int updateTier(MembershipTierUpdateRequest request) {
        if (request == null || request.getTierId() == null) {
            throw new ValidationException(messages.get("backend.tier.notFound"));
        }
        MembershipTier tier = getTierById(request.getTierId());
        if (request.getVersion() == null) {
            throw new ValidationException("version", messages.get("backend.tier.versionRequired"));
        }
        if (!Objects.equals(request.getVersion(), tier.getVersion())) {
            throw new ConflictException(messages.get("backend.tier.concurrentConflict"));
        }

        if (request.getDiscountPercent() == null) {
            throw new ValidationException("discountPercent", messages.get("backend.tier.discountRequired"));
        }
        if (request.getDiscountPercent().compareTo(BigDecimal.ZERO) < 0
                || request.getDiscountPercent().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new ValidationException("discountPercent", messages.get("backend.tier.discountRange"));
        }
        if (request.getDiscountPercent().scale() > 2) {
            throw new ValidationException("discountPercent", messages.get("backend.tier.discountPrecision"));
        }
        if (request.getBorrowLimit() == null || request.getBorrowLimit() < 1
                || request.getBorrowLimit() > 100) {
            throw new ValidationException("borrowLimit", messages.get("backend.tier.borrowLimitRange"));
        }
        if (request.getCondition() == null) {
            throw new ValidationException("condition", messages.get("backend.tier.conditionRequired"));
        }
        if (request.getCondition().compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("condition", messages.get("backend.tier.conditionNonNegative"));
        }
        if (request.getCondition().scale() > 2
                || request.getCondition().compareTo(new BigDecimal("9999999999999999.99")) > 0) {
            throw new ValidationException("condition", messages.get("backend.tier.conditionPrecision"));
        }
        tier.setDiscountPercent(request.getDiscountPercent());
        tier.setBorrowLimit(request.getBorrowLimit());
        tier.setCondition(request.getCondition());

        List<MembershipTier> proposed = new ArrayList<>(getAllTiers());
        proposed.sort(Comparator.comparing(MembershipTier::getCondition)
                .thenComparing(MembershipTier::getTierId));
        validateTierProgression(proposed);

        try {
            membershipTierRepository.saveAndFlush(tier);
        } catch (OptimisticLockingFailureException e) {
            throw new ConflictException(messages.get("backend.tier.concurrentConflict"), e);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException(messages.get("backend.tier.conditionConflict"), e);
        }
        int synchronizedMembers = synchronizeAllMemberTiers();
        auditLogService.log(ActionType.UPDATE_MEMBERSHIP_TIER,
                messages.get("backend.tier.auditUpdated", tier.getTierName(), synchronizedMembers));
        return synchronizedMembers;
    }

    private void validateTierProgression(List<MembershipTier> tiers) {
        if (tiers.isEmpty() || tiers.get(0).getCondition().compareTo(BigDecimal.ZERO) != 0) {
            throw new ValidationException(messages.get("backend.tier.baseMustBeZero"));
        }
        for (int i = 1; i < tiers.size(); i++) {
            MembershipTier previous = tiers.get(i - 1);
            MembershipTier current = tiers.get(i);
            if (current.getCondition().compareTo(previous.getCondition()) <= 0) {
                throw new ValidationException(messages.get("backend.tier.conditionsStrictlyIncreasing"));
            }
            if (current.getDiscountPercent().compareTo(previous.getDiscountPercent()) < 0
                    || current.getBorrowLimit() < previous.getBorrowLimit()) {
                throw new ValidationException(messages.get("backend.tier.benefitsMustNotDecrease"));
            }
        }
    }

    @Override
    @Transactional
    public void synchronizeMemberTier(Integer memberId) {
        if (memberId == null) return;
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException(messages.get("backend.member.currentNotFound")));
        MembershipTier achieved = determineTier(getAccumulatedSpending(member), getAllTiers());
        if (achieved != null && (member.getTier() == null
                || !achieved.getTierId().equals(member.getTier().getTierId()))) {
            member.setTier(achieved);
            memberRepository.save(member);
        }
    }

    @Override
    @Transactional
    public int synchronizeAllMemberTiers() {
        List<MembershipTier> tiers = getAllTiers();
        Map<Integer, BigDecimal> spendingByMember = new HashMap<>();
        for (Object[] row : transactionRepository.sumCompletedMembershipSpendForAllMembers()) {
            if (row[0] instanceof Number memberId && row[1] instanceof BigDecimal spending) {
                spendingByMember.put(memberId.intValue(), spending);
            }
        }
        int changed = 0;
        for (Member member : memberRepository.findAll()) {
            MembershipTier achieved = determineTier(
                    spendingByMember.getOrDefault(member.getMemberId(), BigDecimal.ZERO), tiers);
            if (achieved != null && (member.getTier() == null
                    || !achieved.getTierId().equals(member.getTier().getTierId()))) {
                member.setTier(achieved);
                changed++;
            }
        }
        return changed;
    }

    private MembershipTier determineTier(BigDecimal spending, List<MembershipTier> tiers) {
        MembershipTier achieved = null;
        BigDecimal safeSpending = spending == null ? BigDecimal.ZERO : spending;
        for (MembershipTier candidate : tiers) {
            if (candidate.getCondition() != null && candidate.getCondition().compareTo(safeSpending) <= 0) {
                achieved = candidate;
            }
        }
        return achieved;
    }

    @Override
    @Transactional
    public void deleteTier(Integer id) {
        MembershipTier tier = getTierById(id);
        List<MembershipTier> tiers = membershipTierRepository.findAllByOrderByConditionAsc();
        if (tiers.size() <= 1) {
            throw new ConflictException(messages.get("backend.tier.deleteLast"));
        }
        if (!tiers.isEmpty() && Objects.equals(tiers.get(0).getTierId(), tier.getTierId())) {
            throw new ConflictException(messages.get("backend.tier.deleteBase"));
        }
        // Dùng existsByTier_TierId thay vì findAll().stream() — O(1), không load toàn bộ bảng
        if (memberRepository.existsByTier_TierId(id)) {
            throw new ConflictException(messages.get("backend.tier.deleteInUse"));
        }
        List<MembershipTier> remaining = tiers.stream()
                .filter(candidate -> !Objects.equals(candidate.getTierId(), id))
                .toList();
        validateTierProgression(remaining);
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
