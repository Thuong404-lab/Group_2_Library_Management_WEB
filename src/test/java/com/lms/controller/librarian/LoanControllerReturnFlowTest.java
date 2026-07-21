package com.lms.controller.librarian;

import com.lms.repository.BorrowDetailRepository;
import com.lms.repository.BorrowRepository;
import com.lms.repository.MemberAccountRepository;
import com.lms.repository.MemberRepository;
import com.lms.repository.TransactionRepository;
import com.lms.repository.WalletRepository;
import com.lms.service.FinancialService;
import com.lms.service.LoanService;
import com.lms.service.PayOsPaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LoanControllerReturnFlowTest {

    private LoanService loanService;
    private LoanController controller;

    @BeforeEach
    void setUp() {
        loanService = mock(LoanService.class);
        controller = new LoanController(
                loanService,
                mock(FinancialService.class),
                mock(MemberRepository.class),
                mock(BorrowRepository.class),
                mock(BorrowDetailRepository.class),
                mock(TransactionRepository.class),
                mock(WalletRepository.class),
                mock(MemberAccountRepository.class),
                mock(PayOsPaymentService.class));
    }

    @Test
    void barcodeApiSearchesOnlyTheScannedPhysicalCopy() {
        when(loanService.findActiveLoansByBarcode("BC-001")).thenReturn(List.of());

        ResponseEntity<?> response = controller.apiSearchActiveReturnLoans(" BC-001 ");

        assertEquals(404, response.getStatusCode().value());
        verify(loanService).findActiveLoansByBarcode("BC-001");
        verify(loanService, never()).searchActiveLoansByQuery(any());
    }

    @Test
    void deskReturnRejectsBackdatedDateBeforeCallingService() {
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        String view = controller.confirmReturnBookWithDetails(
                List.of("BC-001"), LocalDate.now().minusDays(1),
                "Tốt - Sách nguyên vẹn, sạch đẹp", null,
                BigDecimal.ZERO, "cash", null, redirect);

        assertEquals("redirect:/librarian/loan/returns", view);
        verify(loanService, never()).confirmBatchReturnWithDetails(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void damagedBookRequiresDescriptionBeforeCallingService() {
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        controller.confirmReturnBookWithDetails(
                List.of("BC-001"), LocalDate.now(),
                "Hư hỏng nặng - Rách trang", "  ",
                BigDecimal.ZERO, "cash", null, redirect);

        verify(loanService, never()).confirmBatchReturnWithDetails(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void fractionalDamageFineIsRejectedBeforeCallingService() {
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        controller.confirmReturnBookWithDetails(
                List.of("BC-001"), LocalDate.now(),
                "Hư hỏng nhẹ - Rách nhỏ", "Rách nhẹ ở mép trang",
                new BigDecimal("10.5"), "cash", null, redirect);

        verify(loanService, never()).confirmBatchReturnWithDetails(any(), any(), any(), any(), any(), any(), any());
    }
}
