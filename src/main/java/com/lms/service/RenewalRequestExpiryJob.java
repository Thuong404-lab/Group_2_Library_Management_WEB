package com.lms.service;

import com.lms.entity.BorrowDetail;
import com.lms.repository.BorrowDetailRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class RenewalRequestExpiryJob {
    private static final Logger log = LoggerFactory.getLogger(RenewalRequestExpiryJob.class);

    private final BorrowDetailRepository borrowDetailRepository;
    private final LoanService loanService;

    public RenewalRequestExpiryJob(BorrowDetailRepository borrowDetailRepository, LoanService loanService) {
        this.borrowDetailRepository = borrowDetailRepository;
        this.loanService = loanService;
    }

    @Scheduled(fixedDelayString = "${lms.renewal-expiry.fixed-delay-ms:60000}",
               initialDelayString = "${lms.renewal-expiry.initial-delay-ms:30000}")
    public void expireOverdueRenewalRequests() {
        for (BorrowDetail detail : borrowDetailRepository
                .findByStatusIgnoreCaseAndDueDateLessThanEqual("Renew_Pending", LocalDateTime.now())) {
            try {
                loanService.rejectRenewal(detail.getBorrowDetailId(), "SYSTEM", "AUTO_EXPIRED");
            } catch (RuntimeException exception) {
                log.warn("Could not expire renewal request {}: {}", detail.getBorrowDetailId(), exception.getMessage());
            }
        }
    }
}