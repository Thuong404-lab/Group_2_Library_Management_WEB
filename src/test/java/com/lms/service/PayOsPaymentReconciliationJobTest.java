package com.lms.service;

import com.lms.repository.PayOsPaymentRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

class PayOsPaymentReconciliationJobTest {

    @Test
    void recoversARecentlyPaidOrderEvenWhenItsLocalStatusExpired() {
        PayOsPaymentRepository paymentRepository = mock(PayOsPaymentRepository.class);
        PayOsPaymentService paymentService = mock(PayOsPaymentService.class);
        PayOsPaymentAuditService auditService = mock(PayOsPaymentAuditService.class);
        when(paymentRepository.findRecoverableOrderCodes(anyList(), any()))
                .thenReturn(List.of(120L));

        PayOsPaymentReconciliationJob job =
                new PayOsPaymentReconciliationJob(paymentRepository, paymentService, auditService);

        job.recoverRecentPaymentsAfterRestart();

        verify(paymentService).reconcileForStaff(120L);
        verify(auditService).resolveReconciliationIssue(120L, "SCHEDULED_JOB");
    }

    @Test
    void keepsRetryingRecoverablePaymentsAfterApplicationRecovers() {
        PayOsPaymentRepository paymentRepository = mock(PayOsPaymentRepository.class);
        PayOsPaymentService paymentService = mock(PayOsPaymentService.class);
        PayOsPaymentAuditService auditService = mock(PayOsPaymentAuditService.class);
        when(paymentRepository.findRecoverableOrderCodes(anyList(), any()))
                .thenReturn(List.of(101L, 102L));

        PayOsPaymentReconciliationJob job =
                new PayOsPaymentReconciliationJob(paymentRepository, paymentService, auditService);

        job.reconcileRecoverablePayments();

        verify(paymentService).reconcileForStaff(101L);
        verify(paymentService).reconcileForStaff(102L);
    }

    @Test
    void persistsAnIssueWhenGatewayReconciliationFails() {
        PayOsPaymentRepository paymentRepository = mock(PayOsPaymentRepository.class);
        PayOsPaymentService paymentService = mock(PayOsPaymentService.class);
        PayOsPaymentAuditService auditService = mock(PayOsPaymentAuditService.class);
        when(paymentRepository.findRecoverableOrderCodes(anyList(), any())).thenReturn(List.of(404L));
        doThrow(new RuntimeException("gateway timeout")).when(paymentService).reconcileForStaff(404L);
        PayOsPaymentReconciliationJob job =
                new PayOsPaymentReconciliationJob(paymentRepository, paymentService, auditService);

        job.reconcileRecoverablePayments();

        verify(auditService).recordReconciliationFailure(404L, "gateway timeout", "SCHEDULED_JOB");
    }
}
