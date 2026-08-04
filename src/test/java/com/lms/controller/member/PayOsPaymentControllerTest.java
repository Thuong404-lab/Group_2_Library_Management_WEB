package com.lms.controller.member;

import com.lms.entity.Member;
import com.lms.entity.PayOsPayment;
import com.lms.repository.MemberRepository;
import com.lms.service.PayOsPaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.security.Principal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayOsPaymentControllerTest {
    @Mock PayOsPaymentService paymentService;
    @Mock MemberRepository memberRepository;

    private PayOsPaymentController controller;

    @BeforeEach
    void setUp() {
        controller = new PayOsPaymentController(paymentService, memberRepository);
    }

    @Test
    void cancelFinePaymentUsesFineCancellationFlow() {
        Member member = new Member();
        member.setMemberId(7);
        PayOsPayment payment = new PayOsPayment();
        payment.setPurpose(PayOsPaymentService.FINE_BATCH);
        Principal principal = () -> "member01";

        when(memberRepository.findByAccountUsername("member01")).thenReturn(Optional.of(member));
        when(paymentService.getForMember(123L, 7)).thenReturn(payment);

        String result = controller.cancelPayment(
                123L, principal, new RedirectAttributesModelMap());

        assertThat(result).isEqualTo("redirect:/member/financial/transactions");
        verify(paymentService).cancelFinePaymentForMember(123L, 7);
        verify(paymentService, never()).cancelBorrowFeeForMember(123L, 7);
    }
}
