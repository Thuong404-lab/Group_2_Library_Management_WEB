package com.lms.service;

import com.lms.dto.request.MembershipTierUpdateRequest;
import com.lms.entity.Member;
import com.lms.entity.MembershipTier;
import com.lms.exception.ValidationException;
import com.lms.exception.ConflictException;
import com.lms.repository.MemberAccountRepository;
import com.lms.repository.MemberRepository;
import com.lms.repository.MembershipTierRepository;
import com.lms.repository.MembershipTierTranslationRepository;
import com.lms.repository.TransactionRepository;
import com.lms.service.impl.MembershipServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class MembershipServiceImplTest {
    private MemberRepository memberRepository;
    private MembershipTierRepository tierRepository;
    private MembershipTierTranslationRepository translationRepository;
    private TransactionRepository transactionRepository;
    private MembershipServiceImpl service;

    @BeforeEach
    void setUp() {
        memberRepository = mock(MemberRepository.class);
        tierRepository = mock(MembershipTierRepository.class);
        translationRepository = mock(MembershipTierTranslationRepository.class);
        transactionRepository = mock(TransactionRepository.class);
        LocalizedMessageService messages = mock(LocalizedMessageService.class);
        when(messages.get(anyString(), any(Object[].class))).thenAnswer(i -> i.getArgument(0));
        when(messages.get(anyString())).thenAnswer(i -> i.getArgument(0));
        when(translationRepository.findByLanguageCode(anyString())).thenReturn(List.of());
        service = new MembershipServiceImpl(
                messages,
                memberRepository,
                tierRepository,
                translationRepository,
                mock(MemberAccountRepository.class),
                transactionRepository,
                mock(AuditLogService.class));
    }

    @Test
    void accumulatedSpendingUsesCompletedServiceSpendQuery() {
        Member member = new Member();
        member.setMemberId(7);
        when(transactionRepository.sumCompletedMembershipSpendByMemberId(7))
                .thenReturn(new BigDecimal("125000.00"));

        assertThat(service.getAccumulatedSpending(member)).isEqualByComparingTo("125000.00");
        verify(transactionRepository).sumCompletedMembershipSpendByMemberId(7);
    }

    @Test
    void synchronizationSelectsHighestReachedTier() {
        MembershipTier base = tier(1, "Member", "0", "0", 5);
        MembershipTier silver = tier(2, "Silver", "500000", "5", 7);
        MembershipTier gold = tier(3, "Gold", "2000000", "10", 10);
        Member member = new Member();
        member.setMemberId(4);
        member.setTier(base);
        when(memberRepository.findById(4)).thenReturn(Optional.of(member));
        when(tierRepository.findAllByOrderByConditionAsc()).thenReturn(List.of(base, silver, gold));
        when(transactionRepository.sumCompletedMembershipSpendByMemberId(4))
                .thenReturn(new BigDecimal("750000"));

        service.synchronizeMemberTier(4);

        assertThat(member.getTier()).isSameAs(silver);
        verify(memberRepository).save(member);
    }

    @Test
    void updateRejectsDuplicateOrInvertedThresholds() {
        MembershipTier base = tier(1, "Member", "0", "0", 5);
        MembershipTier silver = tier(2, "Silver", "500000", "5", 7);
        when(tierRepository.findById(2)).thenReturn(Optional.of(silver));
        when(tierRepository.findAllByOrderByConditionAsc()).thenReturn(List.of(base, silver));

        MembershipTierUpdateRequest request = new MembershipTierUpdateRequest();
        request.setTierId(2);
        request.setVersion(0L);
        request.setCondition(BigDecimal.ZERO);
        request.setDiscountPercent(new BigDecimal("5"));
        request.setBorrowLimit(7);

        assertThatThrownBy(() -> service.updateTier(request))
                .isInstanceOf(ValidationException.class);
        verify(tierRepository, never()).save(any());
    }

    @Test
    void updateRejectsStaleVersionBeforeChangingTier() {
        MembershipTier silver = tier(2, "Silver", "500000", "5", 7);
        silver.setVersion(3L);
        when(tierRepository.findById(2)).thenReturn(Optional.of(silver));

        MembershipTierUpdateRequest request = new MembershipTierUpdateRequest();
        request.setTierId(2);
        request.setVersion(2L);
        request.setCondition(new BigDecimal("500000"));
        request.setDiscountPercent(new BigDecimal("5"));
        request.setBorrowLimit(7);

        assertThatThrownBy(() -> service.updateTier(request))
                .isInstanceOf(ConflictException.class);
        verify(tierRepository, never()).saveAndFlush(any());
    }

    @Test
    void synchronizeAllUsesSingleAggregatedSpendingQuery() {
        MembershipTier base = tier(1, "Member", "0", "0", 5);
        MembershipTier silver = tier(2, "Silver", "500000", "5", 7);
        Member first = new Member();
        first.setMemberId(10);
        first.setTier(base);
        Member second = new Member();
        second.setMemberId(11);
        second.setTier(base);
        when(tierRepository.findAllByOrderByConditionAsc()).thenReturn(List.of(base, silver));
        when(memberRepository.findAll()).thenReturn(List.of(first, second));
        when(transactionRepository.sumCompletedMembershipSpendForAllMembers())
                .thenReturn(List.<Object[]>of(new Object[]{10, new BigDecimal("750000")}));

        assertThat(service.synchronizeAllMemberTiers()).isEqualTo(1);
        assertThat(first.getTier()).isSameAs(silver);
        assertThat(second.getTier()).isSameAs(base);
        verify(transactionRepository, never()).sumCompletedMembershipSpendByMemberId(anyInt());
    }

    @Test
    void deleteRejectsBaseTierEvenWhenUnused() {
        MembershipTier base = tier(1, "Member", "0", "0", 5);
        MembershipTier silver = tier(2, "Silver", "500000", "5", 7);
        when(tierRepository.findById(1)).thenReturn(Optional.of(base));
        when(tierRepository.findAllByOrderByConditionAsc()).thenReturn(List.of(base, silver));

        assertThatThrownBy(() -> service.deleteTier(1)).isInstanceOf(ConflictException.class);
        verify(tierRepository, never()).delete(any());
    }

    private MembershipTier tier(int id, String name, String condition, String discount, int limit) {
        MembershipTier tier = new MembershipTier(id, name, new BigDecimal(discount), limit,
                new BigDecimal(condition), null);
        tier.setVersion(0L);
        return tier;
    }
}
