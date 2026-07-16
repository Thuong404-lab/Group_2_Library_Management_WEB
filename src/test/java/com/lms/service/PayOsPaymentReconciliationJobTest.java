package com.lms.service;

import com.lms.repository.PayOsPaymentRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PayOsPaymentReconciliationJobTest {

    @Test
    void recoversARecentlyPaidOrderEvenWhenItsLocalStatusExpired() {
        PayOsPaymentRepository paymentRepository = mock(PayOsPaymentRepository.class);
        PayOsPaymentService paymentService = mock(PayOsPaymentService.class);
        when(paymentRepository.findRecoverableOrderCodes(anyList(), any()))
                .thenReturn(List.of(120L));

        PayOsPaymentReconciliationJob job =
                new PayOsPaymentReconciliationJob(paymentRepository, paymentService);

        job.recoverRecentPaymentsAfterRestart();

        verify(paymentService).refreshForStaff(120L);
    }

    @Test
    void keepsRetryingRecoverablePaymentsAfterApplicationRecovers() {
        PayOsPaymentRepository paymentRepository = mock(PayOsPaymentRepository.class);
        PayOsPaymentService paymentService = mock(PayOsPaymentService.class);
        when(paymentRepository.findRecoverableOrderCodes(anyList(), any()))
                .thenReturn(List.of(101L, 102L));

        PayOsPaymentReconciliationJob job =
                new PayOsPaymentReconciliationJob(paymentRepository, paymentService);

        job.reconcileRecoverablePayments();

        verify(paymentService).refreshForStaff(101L);
        verify(paymentService).refreshForStaff(102L);
    }
}
