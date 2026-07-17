package com.lms.service;

import com.lms.entity.Member;
import com.lms.entity.Notification;
import com.lms.entity.Reservation;
import com.lms.entity.Transaction;
import com.lms.entity.Wallet;
import com.lms.repository.BorrowDetailRepository;
import com.lms.repository.BorrowRepository;
import com.lms.repository.MemberNotificationRepository;
import com.lms.repository.MemberRepository;
import com.lms.repository.NotificationRepository;
import com.lms.repository.ReservationRepository;
import com.lms.repository.SystemSettingRepository;
import com.lms.repository.TransactionRepository;
import com.lms.repository.WalletRepository;
import com.lms.service.impl.FinancialServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.i18n.LocaleContextHolder;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FinancialServiceRefundTest {
    private TransactionRepository transactionRepository;
    private WalletRepository walletRepository;
    private SystemSettingRepository systemSettingRepository;
    private NotificationRepository notificationRepository;
    private MemberNotificationRepository memberNotificationRepository;
    private ReservationRepository reservationRepository;
    private FinancialService service;

    @BeforeEach
    void setUp() {
        LocaleContextHolder.setLocale(Locale.forLanguageTag("vi"));
        transactionRepository = mock(TransactionRepository.class);
        walletRepository = mock(WalletRepository.class);
        systemSettingRepository = mock(SystemSettingRepository.class);
        notificationRepository = mock(NotificationRepository.class);
        memberNotificationRepository = mock(MemberNotificationRepository.class);
        reservationRepository = mock(ReservationRepository.class);

        service = new FinancialServiceImpl(
                transactionRepository,
                walletRepository,
                mock(BorrowRepository.class),
                mock(BorrowDetailRepository.class),
                systemSettingRepository,
                mock(MemberRepository.class),
                notificationRepository,
                memberNotificationRepository,
                reservationRepository);
    }

    @AfterEach
    void resetLocale() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void refundsDepositToWalletAndWritesRefundTransaction() {
        Member member = new Member();
        member.setMemberId(7);
        Wallet wallet = new Wallet(3, member, new BigDecimal("120000"));
        Reservation reservation = new Reservation(14, member, null, null, "Refund_Pending");

        when(reservationRepository.findByIdForUpdate(14)).thenReturn(Optional.of(reservation));
        when(walletRepository.findByMemberMemberId(7)).thenReturn(Optional.of(wallet));
        when(systemSettingRepository.findBySettingKeyIgnoreCase("Deposit_Amount")).thenReturn(Optional.empty());
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            notification.setNotificationId(21);
            return notification;
        });

        service.refundReservationDeposit(7, 14);

        assertEquals(new BigDecimal("170000"), wallet.getBalance());
        assertEquals("Refunded", reservation.getStatus());
        verify(walletRepository).save(wallet);
        verify(reservationRepository).save(reservation);

        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(transactionCaptor.capture());
        assertEquals("REFUND", transactionCaptor.getValue().getTransactionType());
        assertEquals(new BigDecimal("50000"), transactionCaptor.getValue().getAmount());
        assertEquals("Completed", transactionCaptor.getValue().getStatus());
        verify(memberNotificationRepository).save(any());
    }

    @Test
    void memberRequestWaitsForLibrarianApprovalWithoutChangingWallet() {
        Member member = new Member();
        member.setMemberId(7);
        Reservation reservation = new Reservation(14, member, null, null, "Deposit_Paid");
        when(reservationRepository.findByIdForUpdate(14)).thenReturn(Optional.of(reservation));

        service.requestReservationDepositRefund(7, 14);

        assertEquals("Refund_Pending", reservation.getStatus());
        verify(reservationRepository).save(reservation);
        verify(walletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void rejectsAReservationThatWasAlreadyRefunded() {
        Member member = new Member();
        member.setMemberId(7);
        Reservation reservation = new Reservation(14, member, null, null, "Refunded");
        when(reservationRepository.findByIdForUpdate(14)).thenReturn(Optional.of(reservation));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> service.refundReservationDeposit(7, 14));

        assertEquals("Tiền cọc của phiếu này đã được hoàn trước đó.", exception.getMessage());
        verify(walletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }
}
