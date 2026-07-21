package com.lms.service.impl;

import com.lms.dto.response.LibrarianRevenueReportData;
import com.lms.exception.ValidationException;
import com.lms.repository.BookItemRepository;
import com.lms.repository.BookRepository;
import com.lms.repository.BorrowDetailRepository;
import com.lms.repository.BorrowRepository;
import com.lms.repository.MemberRepository;
import com.lms.repository.TransactionRepository;
import com.lms.util.FinancialTransactionPolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {
    @Mock BorrowRepository borrowRepository;
    @Mock BorrowDetailRepository borrowDetailRepository;
    @Mock TransactionRepository transactionRepository;
    @Mock MemberRepository memberRepository;
    @Mock BookRepository bookRepository;
    @Mock BookItemRepository bookItemRepository;

    @InjectMocks ReportServiceImpl service;

    @Test
    void librarianReportUsesCompletedRevenueTypesAndKeepsRefundsSeparate() {
        LocalDate from = LocalDate.of(2026, 7, 1);
        LocalDate to = LocalDate.of(2026, 7, 22);
        when(transactionRepository.summarizeRevenueByTypeAndDateRange(
                eq(FinancialTransactionPolicy.COMPLETED_STATUS),
                eq(FinancialTransactionPolicy.REVENUE_TYPES), any(), any()))
                .thenReturn(List.of(
                        new Object[] { "BORROW_FEE", 2L, new BigDecimal("10000") },
                        new Object[] { "RENEWAL_FEE", 1L, new BigDecimal("5000") }));
        when(transactionRepository.sumRevenueByStatusAndTypesAndDateRange(
                eq(FinancialTransactionPolicy.COMPLETED_STATUS),
                eq(FinancialTransactionPolicy.REVENUE_TYPES), any(), any()))
                .thenReturn(new BigDecimal("15000"));
        when(transactionRepository.sumAbsoluteAmountByTypeAndStatusAndDateRange(
                eq(FinancialTransactionPolicy.REFUND_TYPE),
                eq(FinancialTransactionPolicy.COMPLETED_STATUS), any(), any()))
                .thenReturn(new BigDecimal("20000"));
        when(transactionRepository.summarizeMonthlyRevenueByTypesAndDateRange(
                eq(FinancialTransactionPolicy.COMPLETED_STATUS),
                eq(FinancialTransactionPolicy.REVENUE_TYPES), any(), any()))
                .thenReturn(List.<Object[]>of(new Object[] { 7, 2026, 3L, new BigDecimal("15000") }));

        LibrarianRevenueReportData report = service.getLibrarianRevenueReport(from, to);

        assertThat(report.getTotalRevenue()).isEqualByComparingTo("15000");
        assertThat(report.getTotalRefunds()).isEqualByComparingTo("20000");
        assertThat(report.getTotalTransactions()).isEqualTo(3);
        assertThat(report.getAverageTransaction()).isEqualByComparingTo("5000.00");
        assertThat(report.getTransactionBreakdown()).extracting("label")
                .containsExactly("Borrowing Fee", "Renewal fee");
    }

    @Test
    void rejectsReversedAndExcessiveDateRanges() {
        assertThatThrownBy(() -> service.getLibrarianRevenueReport(
                LocalDate.of(2026, 7, 22), LocalDate.of(2026, 7, 1)))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> service.getLibrarianRevenueReport(
                LocalDate.of(2020, 1, 1), LocalDate.of(2026, 1, 1)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsUnsupportedExportBeforeReadingReportData() {
        assertThatThrownBy(() -> service.exportLibrarianRevenueReport(
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 22), "xlsx"))
                .isInstanceOf(ValidationException.class);
        verify(transactionRepository, never()).summarizeRevenueByTypeAndDateRange(
                any(), any(), any(LocalDateTime.class), any(LocalDateTime.class));
    }
}
