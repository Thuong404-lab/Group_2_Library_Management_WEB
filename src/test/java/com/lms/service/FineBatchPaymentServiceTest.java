package com.lms.service;

import com.lms.entity.Member;
import com.lms.entity.Notification;
import com.lms.entity.Staff;
import com.lms.entity.Transaction;
import com.lms.entity.Wallet;
import com.lms.exception.ConflictException;
import com.lms.repository.MemberNotificationRepository;
import com.lms.repository.NotificationRepository;
import com.lms.repository.TransactionRepository;
import com.lms.repository.payos.PayOsTransactionRepository;
import com.lms.repository.payos.PayOsWalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class FineBatchPaymentServiceTest {
    private TransactionRepository transactionRepository;
    private PayOsTransactionRepository lockedTransactionRepository;
    private FineBatchPaymentService service;

    @BeforeEach
    void setUp() {
        transactionRepository = mock(TransactionRepository.class);
        lockedTransactionRepository = mock(PayOsTransactionRepository.class);
        NotificationRepository notificationRepository = mock(NotificationRepository.class);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            notification.setNotificationId(1);
            return notification;
        });
        service = new FineBatchPaymentService(
                transactionRepository,
                lockedTransactionRepository,
                mock(PayOsWalletRepository.class),
                notificationRepository,
                mock(MemberNotificationRepository.class));
    }

    @Test
    void cashPaymentRecordsStaffChannelReferenceAndPaidTime() {
        Transaction fine = pendingFine(41);
        Staff staff = new Staff();
        staff.setStaffId(7);
        when(transactionRepository.findPendingFineTransactionsByBorrowId(9, List.of("FINE", "DAMAGE_FEE")))
                .thenReturn(List.of(fine));
        when(lockedTransactionRepository.findByIdForUpdate(41)).thenReturn(Optional.of(fine));

        service.payBorrowFinesByCash(9, staff);

        assertThat(fine.getStatus()).isEqualTo("Completed");
        assertThat(fine.getChannel()).isEqualTo("CASH");
        assertThat(fine.getPerformedByStaff()).isSameAs(staff);
        assertThat(fine.getReferenceCode()).isEqualTo("CASH-FINE-41");
        assertThat(fine.getPaidAt()).isNotNull();
        verify(lockedTransactionRepository).save(fine);
    }

    @Test
    void refusesFineThatChangedFromPendingBeforeLock() {
        Transaction fine = pendingFine(42);
        fine.setStatus("Cancelled");
        Staff staff = new Staff();
        staff.setStaffId(7);
        when(transactionRepository.findPendingFineTransactionsByBorrowId(9, List.of("FINE", "DAMAGE_FEE")))
                .thenReturn(List.of(fine));
        when(lockedTransactionRepository.findByIdForUpdate(42)).thenReturn(Optional.of(fine));

        assertThatThrownBy(() -> service.payBorrowFinesByCash(9, staff))
                .isInstanceOf(ConflictException.class);
        verify(lockedTransactionRepository, never()).save(any());
    }

    private Transaction pendingFine(int id) {
        Member member = new Member();
        member.setMemberId(3);
        Wallet wallet = new Wallet();
        wallet.setMember(member);
        Transaction fine = new Transaction();
        fine.setTransactionId(id);
        fine.setWallet(wallet);
        fine.setTransactionType("FINE");
        fine.setStatus("Pending");
        fine.setAmount(new BigDecimal("40000"));
        return fine;
    }
}
