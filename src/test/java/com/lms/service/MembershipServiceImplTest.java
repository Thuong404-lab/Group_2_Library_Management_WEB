package com.lms.service;

import com.lms.dto.request.MembershipTierUpdateRequest;
import com.lms.entity.Member;
import com.lms.entity.MembershipTier;
import com.lms.exception.ValidationException;
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
        request.setCondition(BigDecimal.ZERO);
        request.setDiscountPercent(new BigDecimal("5"));
        request.setBorrowLimit(7);

        assertThatThrownBy(() -> service.updateTier(request, "en"))
                .isInstanceOf(ValidationException.class);
        verify(tierRepository, never()).save(any());
    }

    private MembershipTier tier(int id, String name, String condition, String discount, int limit) {
        return new MembershipTier(id, name, new BigDecimal(discount), limit,
                new BigDecimal(condition), null);
    }
}
