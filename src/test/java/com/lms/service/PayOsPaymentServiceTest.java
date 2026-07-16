package com.lms.service;

import com.lms.entity.PayOsPayment;
import com.lms.entity.PayOsPaymentFineItem;
import com.lms.entity.Transaction;
import com.lms.repository.BorrowRepository;
import com.lms.repository.PayOsPaymentFineItemRepository;
import com.lms.repository.PayOsPaymentRepository;
import com.lms.repository.TransactionRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PayOsPaymentServiceTest {

    @Test
    void qrExpiresExactlyFiveMinutesAfterPaymentCreation() {
        PayOsPaymentService service = service(mock(PayOsPaymentFineItemRepository.class));
        PayOsPayment payment = payment(3L);
        LocalDateTime createdAt = LocalDateTime.of(2026, 7, 14, 23, 30);
        payment.setCreatedAt(createdAt);

        long expected = createdAt.plusMinutes(5)
                .atZone(ZoneId.of("Asia/Ho_Chi_Minh"))
                .toInstant().toEpochMilli();

        assertThat(service.getExpiryEpochMillis(payment)).isEqualTo(expected);
    }

    @Test
    void rejectsPendingBatchWhenANewFineWasAdded() {
        PayOsPaymentFineItemRepository fineItemRepository = mock(PayOsPaymentFineItemRepository.class);
        PayOsPaymentService service = service(fineItemRepository);
        PayOsPayment payment = payment(1L);

        when(fineItemRepository.findByPaymentPaymentIdOrderByFineTransactionTransactionId(1L))
                .thenReturn(List.of(item(fine(23, "10000"), "10000")));

        boolean matches = service.sameFineSnapshot(payment,
                List.of(fine(23, "10000"), fine(24, "100000")));

        assertThat(matches).isFalse();
    }

    @Test
    void reusesPendingBatchOnlyWhenIdsAndAmountsAreIdentical() {
        PayOsPaymentFineItemRepository fineItemRepository = mock(PayOsPaymentFineItemRepository.class);
        PayOsPaymentService service = service(fineItemRepository);
        PayOsPayment payment = payment(2L);

        when(fineItemRepository.findByPaymentPaymentIdOrderByFineTransactionTransactionId(2L))
                .thenReturn(List.of(
                        item(fine(23, "10000"), "10000"),
                        item(fine(24, "100000"), "100000")));

        boolean matches = service.sameFineSnapshot(payment,
                List.of(fine(24, "100000"), fine(23, "10000")));

        assertThat(matches).isTrue();
    }

    private PayOsPaymentService service(PayOsPaymentFineItemRepository fineItemRepository) {
        return new PayOsPaymentService(
                mock(PayOsPaymentRepository.class),
                fineItemRepository,
                mock(TransactionRepository.class),
                mock(BorrowRepository.class),
                mock(FinancialService.class),
                mock(PayOsSettlementService.class),
                mock(PayOsPaymentAuditService.class),
                "", "", "", "http://localhost:8080");
    }

    private PayOsPayment payment(Long id) {
        PayOsPayment payment = new PayOsPayment();
        payment.setPaymentId(id);
        return payment;
    }

    private Transaction fine(Integer id, String amount) {
        Transaction fine = new Transaction();
        fine.setTransactionId(id);
        fine.setAmount(new BigDecimal(amount));
        return fine;
    }

    private PayOsPaymentFineItem item(Transaction fine, String amount) {
        PayOsPaymentFineItem item = new PayOsPaymentFineItem();
        item.setFineTransaction(fine);
        item.setAmountSnapshot(new BigDecimal(amount));
        return item;
    }
}
