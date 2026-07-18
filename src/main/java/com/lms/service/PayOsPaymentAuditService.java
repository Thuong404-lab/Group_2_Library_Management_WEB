package com.lms.service;

import com.lms.config.CustomUserDetails;
import com.lms.entity.PayOsPayment;
import com.lms.entity.PayOsPaymentAuditLog;
import com.lms.entity.PayOsReconciliationIssue;
import com.lms.entity.User;
import com.lms.repository.PayOsPaymentAuditLogRepository;
import com.lms.repository.PayOsPaymentRepository;
import com.lms.repository.PayOsReconciliationIssueRepository;
import com.lms.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class PayOsPaymentAuditService {

    private final LocalizedMessageService messages;
    private final PayOsPaymentAuditLogRepository auditRepository;
    private final PayOsReconciliationIssueRepository issueRepository;
    private final PayOsPaymentRepository paymentRepository;
    private final UserRepository userRepository;

    public PayOsPaymentAuditService(PayOsPaymentAuditLogRepository auditRepository, PayOsReconciliationIssueRepository issueRepository, PayOsPaymentRepository paymentRepository, UserRepository userRepository, LocalizedMessageService messages) {
        this.auditRepository = auditRepository;
        this.issueRepository = issueRepository;
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.messages = messages;
    }

    @Transactional
    public void record(PayOsPayment payment, String eventType, String source,
                       String oldStatus, String newStatus, boolean successful, String message) {
        if (payment == null || payment.getPaymentId() == null) return;
        saveAudit(payment, eventType, source, oldStatus, newStatus, successful, message);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordReconciliationFailure(Long orderCode, String message, String source) {
        PayOsPayment payment = paymentRepository.findByOrderCode(orderCode).orElse(null);
        if (payment == null) return;

        LocalDateTime now = LocalDateTime.now();
        PayOsReconciliationIssue issue = issueRepository
                .findFirstByPaymentPaymentIdAndStatusOrderByLastAttemptAtDesc(
                        payment.getPaymentId(), PayOsReconciliationIssue.OPEN)
                .orElseGet(() -> {
                    PayOsReconciliationIssue created = new PayOsReconciliationIssue();
                    created.setPayment(payment);
                    created.setStatus(PayOsReconciliationIssue.OPEN);
                    created.setFirstSeenAt(now);
                    return created;
                });
        issue.setAttemptCount(issue.getAttemptCount() + 1);
        issue.setLastAttemptAt(now);
        issue.setErrorMessage(safeMessage(message));
        issueRepository.save(issue);
        saveAudit(payment, "RECONCILIATION_FAILED", source, payment.getStatus(), payment.getStatus(), false,
                safeMessage(message));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void resolveReconciliationIssue(Long orderCode, String source) {
        PayOsPayment payment = paymentRepository.findByOrderCode(orderCode).orElse(null);
        if (payment == null) return;
        PayOsReconciliationIssue issue = issueRepository
                .findFirstByPaymentPaymentIdAndStatusOrderByLastAttemptAtDesc(
                        payment.getPaymentId(), PayOsReconciliationIssue.OPEN)
                .orElse(null);
        if (issue == null) return;
        issue.setStatus(PayOsReconciliationIssue.RESOLVED);
        issue.setResolvedAt(LocalDateTime.now());
        issueRepository.save(issue);
        saveAudit(payment, "RECONCILIATION_RESOLVED", source, payment.getStatus(), payment.getStatus(), true,
                messages.get("backend.payment.audit.issueResolved"));
    }

    private void saveAudit(PayOsPayment payment, String eventType, String source,
                           String oldStatus, String newStatus, boolean successful, String message) {
        PayOsPaymentAuditLog audit = new PayOsPaymentAuditLog();
        audit.setPayment(payment);
        audit.setActor(currentActor());
        audit.setEventType(eventType);
        audit.setSource(source == null ? "SYSTEM" : source);
        audit.setOldStatus(oldStatus);
        audit.setNewStatus(newStatus);
        audit.setSuccessful(successful);
        audit.setMessage(safeMessage(message));
        auditRepository.save(audit);
    }

    private User currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails details)
                || details.getUser() == null || details.getUser().getId() == null) return null;
        return userRepository.findById(details.getUser().getId()).orElse(null);
    }

    private String safeMessage(String message) {
        if (message == null || message.isBlank()) return messages.get("backend.common.noDetails");
        return message.length() <= 4000 ? message : message.substring(0, 4000);
    }
}
