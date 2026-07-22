package com.lms.service;

import com.lms.entity.BorrowDetail;
import com.lms.entity.Transaction;
import com.lms.repository.BorrowDetailRepository;
import com.lms.repository.TransactionRepository;
import com.lms.repository.SystemSettingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class RenewalRequestExpiryJob {
    private static final Logger log = LoggerFactory.getLogger(RenewalRequestExpiryJob.class);

    private final BorrowDetailRepository borrowDetailRepository;
    private final TransactionRepository transactionRepository;
    private final LoanService loanService;
    private final SystemSettingRepository systemSettingRepository;

    public RenewalRequestExpiryJob(BorrowDetailRepository borrowDetailRepository,
                                   TransactionRepository transactionRepository,
                                   LoanService loanService,
                                   SystemSettingRepository systemSettingRepository) {
        this.borrowDetailRepository = borrowDetailRepository;
        this.transactionRepository = transactionRepository;
        this.loanService = loanService;
        this.systemSettingRepository = systemSettingRepository;
    }

    @Scheduled(fixedDelayString = "${lms.renewal-expiry.fixed-delay-ms:60000}",
               initialDelayString = "${lms.renewal-expiry.initial-delay-ms:30000}")
    public void expireOverdueRenewalRequests() {
        LocalDateTime now = LocalDateTime.now();
        int timeoutHours = getPositiveIntSetting("RENEWAL_APPROVAL_TIMEOUT_HOURS", 12);
        for (BorrowDetail detail : borrowDetailRepository.findByStatus("Renew_Pending")) {
            try {
                Transaction hold = transactionRepository
                        .findFirstByBorrowDetailBorrowDetailIdAndTransactionTypeIgnoreCaseAndStatusIgnoreCaseOrderByTransactionIdDesc(
                                detail.getBorrowDetailId(), "RENEWAL_FEE", "Pending")
                        .orElse(null);
                if (hold == null || hold.getTransactionDate() == null) {
                    log.warn("Renewal request {} has no pending wallet hold", detail.getBorrowDetailId());
                    detail.setStatus(detail.getDueDate() != null && detail.getDueDate().isBefore(now)
                            ? "Overdue" : "Borrowed");
                    detail.setRejectionCode("OTHER");
                    detail.setRejectionReason("Missing pending renewal fee hold; request recovered automatically.");
                    borrowDetailRepository.save(detail);
                    continue;
                }
                LocalDateTime approvalDeadline = hold.getTransactionDate().plusHours(timeoutHours);
                LocalDateTime dueDeadline = detail.getDueDate();
                LocalDateTime expiresAt = dueDeadline != null && dueDeadline.isBefore(approvalDeadline)
                        ? dueDeadline : approvalDeadline;
                if (!expiresAt.isAfter(now)) {
                    loanService.rejectRenewal(detail.getBorrowDetailId(), "SYSTEM", "APPROVAL_EXPIRED", null);
                }
            } catch (RuntimeException exception) {
                log.warn("Could not expire renewal request {}: {}", detail.getBorrowDetailId(), exception.getMessage());
            }
        }
    }

    private int getPositiveIntSetting(String key, int defaultValue) {
        try {
            return systemSettingRepository.findBySettingKeyIgnoreCase(key)
                    .map(setting -> setting.getSettingValue())
                    .map(String::trim)
                    .map(Integer::parseInt)
                    .filter(value -> value > 0)
                    .orElse(defaultValue);
        } catch (RuntimeException ignored) {
            return defaultValue;
        }
    }

}
