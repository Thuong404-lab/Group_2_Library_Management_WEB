package com.lms.service;

import com.lms.entity.BorrowDetail;
import com.lms.repository.BorrowDetailRepository;
import com.lms.repository.SystemSettingRepository;
import com.lms.repository.TransactionRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

class RenewalRequestExpiryJobTest {

    @Test
    void orphanedPendingRequestIsRecoveredInsteadOfRemainingStuck() {
        BorrowDetailRepository details = mock(BorrowDetailRepository.class);
        TransactionRepository transactions = mock(TransactionRepository.class);
        LoanService loans = mock(LoanService.class);
        SystemSettingRepository settings = mock(SystemSettingRepository.class);
        BorrowDetail detail = new BorrowDetail();
        detail.setBorrowDetailId(41);
        detail.setStatus("Renew_Pending");
        detail.setDueDate(LocalDateTime.now().plusDays(2));

        when(details.findByStatus("Renew_Pending")).thenReturn(List.of(detail));
        when(transactions.findFirstByBorrowDetailBorrowDetailIdAndTransactionTypeIgnoreCaseAndStatusIgnoreCaseOrderByTransactionIdDesc(
                41, "RENEWAL_FEE", "Pending")).thenReturn(Optional.empty());
        when(settings.findBySettingKeyIgnoreCase("RENEWAL_APPROVAL_TIMEOUT_HOURS")).thenReturn(Optional.empty());

        new RenewalRequestExpiryJob(details, transactions, loans, settings).expireOverdueRenewalRequests();

        assertEquals("Borrowed", detail.getStatus());
        assertEquals("OTHER", detail.getRejectionCode());
        assertNotNull(detail.getRejectionReason());
        verify(details).save(detail);
        verifyNoInteractions(loans);
    }
}
