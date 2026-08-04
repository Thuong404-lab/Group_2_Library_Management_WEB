package com.lms.service;

import com.lms.entity.Borrow;
import com.lms.entity.Member;
import com.lms.entity.PayOsPayment;
import com.lms.entity.Staff;
import com.lms.entity.Transaction;
import com.lms.entity.Wallet;
import com.lms.repository.MemberNotificationRepository;
import com.lms.repository.NotificationRepository;
import com.lms.repository.PayOsPaymentFineItemRepository;
import com.lms.repository.payos.PayOsBorrowRepository;
import com.lms.repository.payos.PayOsTransactionRepository;
import com.lms.repository.payos.PayOsWalletRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayOsSettlementBorrowLifecycleTest {
    @Mock PayOsWalletRepository walletRepository;
    @Mock PayOsTransactionRepository transactionRepository;
    @Mock PayOsBorrowRepository borrowRepository;
    @Mock PayOsPaymentFineItemRepository fineItemRepository;
    @Mock FinancialService financialService;
    @Mock NotificationRepository notificationRepository;
    @Mock MemberNotificationRepository memberNotificationRepository;
    @Mock BorrowService borrowService;
    @Mock LoanService loanService;
    @Mock MembershipService membershipService;
    @Mock LocalizedMessageService localizedMessageService;

    @Test
    void memberPayOsPaymentIsHeldAndRoutedToPendingApproval() {
        Member member = new Member();
        member.setMemberId(7);
        Borrow borrow = new Borrow();
        borrow.setBorrowId(42);
        borrow.setMember(member);
        borrow.setStatus("Payment_Pending");
        Wallet wallet = new Wallet();
        wallet.setMember(member);
        wallet.setBalance(BigDecimal.ZERO);

        PayOsPayment payment = new PayOsPayment();
        payment.setMember(member);
        payment.setPurpose(PayOsPaymentService.BORROW_FEE);
        payment.setReferenceId(42);
        payment.setAmount(BigDecimal.valueOf(70_000));
        payment.setOrderCode(123456L);
        payment.setPaidAt(LocalDateTime.now());

        when(borrowRepository.findByIdForUpdate(42)).thenReturn(Optional.of(borrow));
        when(transactionRepository.hasCompletedBorrowFee(7, 42)).thenReturn(false);
        when(transactionRepository
                .findFirstByBorrowBorrowIdAndTransactionTypeIgnoreCaseAndStatusIgnoreCaseOrderByTransactionDateDesc(
                        42, "BORROW_FEE", "Held"))
                .thenReturn(Optional.empty());
        when(financialService.calculateBorrowingFeeAmount(42)).thenReturn(BigDecimal.valueOf(70_000));
        when(walletRepository.findByMemberIdForUpdate(7)).thenReturn(Optional.of(wallet));
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PayOsSettlementService service = new PayOsSettlementService(
                walletRepository, transactionRepository, borrowRepository, fineItemRepository,
                financialService, notificationRepository, memberNotificationRepository,
                borrowService, loanService, membershipService, localizedMessageService);

        Transaction result = service.settle(payment);

        assertThat(result.getStatus()).isEqualTo("Held");
        assertThat(result.getChannel()).isEqualTo("PAYOS");
        assertThat(result.getBorrow()).isSameAs(borrow);
        verify(borrowService).markPendingBankBorrowPaidForApproval(42);
        verify(borrowService, never()).activatePendingBankBorrow(42);
        verify(membershipService, never()).synchronizeMemberTier(7);

        ArgumentCaptor<Transaction> saved = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, org.mockito.Mockito.times(2)).save(saved.capture());
        assertThat(saved.getAllValues()).allSatisfy(transaction ->
                assertThat(transaction.getStatus()).isEqualTo("Held"));
    }

    @Test
    void librarianPayOsPaymentCompletesAndActivatesDeskBorrow() {
        Member member = new Member();
        member.setMemberId(7);
        Staff staff = new Staff();
        staff.setStaffId(3);

        Borrow borrow = new Borrow();
        borrow.setBorrowId(42);
        borrow.setMember(member);
        borrow.setStaff(staff);
        borrow.setStatus("Payment_Pending");

        Wallet wallet = new Wallet();
        wallet.setMember(member);
        wallet.setBalance(BigDecimal.ZERO);

        PayOsPayment payment = new PayOsPayment();
        payment.setMember(member);
        payment.setInitiatedByStaff(staff);
        payment.setPurpose(PayOsPaymentService.BORROW_FEE);
        payment.setReferenceId(42);
        payment.setAmount(BigDecimal.valueOf(70_000));
        payment.setOrderCode(123457L);
        payment.setPaidAt(LocalDateTime.now());

        when(borrowRepository.findByIdForUpdate(42)).thenReturn(Optional.of(borrow));
        when(transactionRepository.hasCompletedBorrowFee(7, 42)).thenReturn(false);
        when(transactionRepository
                .findFirstByBorrowBorrowIdAndTransactionTypeIgnoreCaseAndStatusIgnoreCaseOrderByTransactionDateDesc(
                        42, "BORROW_FEE", "Held"))
                .thenReturn(Optional.empty());
        when(financialService.calculateBorrowingFeeAmount(42)).thenReturn(BigDecimal.valueOf(70_000));
        when(walletRepository.findByMemberIdForUpdate(7)).thenReturn(Optional.of(wallet));
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PayOsSettlementService service = new PayOsSettlementService(
                walletRepository, transactionRepository, borrowRepository, fineItemRepository,
                financialService, notificationRepository, memberNotificationRepository,
                borrowService, loanService, membershipService, localizedMessageService);

        Transaction result = service.settle(payment);

        assertThat(result.getStatus()).isEqualTo("Completed");
        assertThat(result.getPerformedByStaff()).isSameAs(staff);
        verify(borrowService).activatePendingBankBorrow(42);
        verify(borrowService, never()).markPendingBankBorrowPaidForApproval(42);
        verify(membershipService).synchronizeMemberTier(7);
    }
}
