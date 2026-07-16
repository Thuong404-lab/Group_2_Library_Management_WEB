package com.lms.service;

import com.lms.repository.PayOsPaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.time.LocalDateTime;

/**
 * Recovers KQPay payments when a localhost webhook or receipt-page polling was
 * interrupted. Each order is reconciled through the normal transactional flow.
 */
@Service
@ConditionalOnProperty(name = "kqpay.reconciliation.enabled", havingValue = "true", matchIfMissing = true)
public class PayOsPaymentReconciliationJob {
    private static final Logger LOGGER = LoggerFactory.getLogger(PayOsPaymentReconciliationJob.class);

    private final PayOsPaymentRepository paymentRepository;
    private final PayOsPaymentService paymentService;
    private final PayOsPaymentAuditService auditService;

    public PayOsPaymentReconciliationJob(PayOsPaymentRepository paymentRepository,
                                         PayOsPaymentService paymentService,
                                         PayOsPaymentAuditService auditService) {
        this.paymentRepository = paymentRepository;
        this.paymentService = paymentService;
        this.auditService = auditService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recoverRecentPaymentsAfterRestart() {
        reconcile(paymentRepository.findRecoverableOrderCodes(
                List.of(PayOsPaymentService.PENDING, "EXPIRED", "FAILED", PayOsPaymentService.PAID),
                LocalDateTime.now().minusDays(7)));
    }

    @Scheduled(
            initialDelayString = "${kqpay.reconciliation.initial-delay-ms:5000}",
            fixedDelayString = "${kqpay.reconciliation.delay-ms:30000}")
    public void reconcileRecoverablePayments() {
        recoverRecentPayments();
    }

    private void recoverRecentPayments() {
        reconcile(paymentRepository.findRecoverableOrderCodes(
                List.of(PayOsPaymentService.PENDING, "EXPIRED", "FAILED", PayOsPaymentService.PAID),
                LocalDateTime.now().minusDays(7)));
    }

    private void reconcile(List<Long> orderCodes) {
        for (Long orderCode : orderCodes) {
            try {
                paymentService.reconcileForStaff(orderCode);
                auditService.resolveReconciliationIssue(orderCode, "SCHEDULED_JOB");
            } catch (Exception exception) {
                LOGGER.warn("Không thể đối soát đơn KQPay {}: {}", orderCode, exception.getMessage());
                auditService.recordReconciliationFailure(orderCode, exception.getMessage(), "SCHEDULED_JOB");
            }
        }
    }
}
