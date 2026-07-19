package com.lms.service;

import com.lms.dto.request.BorrowRequest;
import com.lms.entity.Book;
import com.lms.entity.BookItem;
import com.lms.entity.Borrow;
import com.lms.entity.BorrowDetail;
import com.lms.entity.Member;
import com.lms.entity.Notification;
import com.lms.entity.User;
import com.lms.enums.UserStatus;
import com.lms.repository.BookItemRepository;
import com.lms.repository.BookRepository;
import com.lms.repository.BorrowDetailRepository;
import com.lms.repository.BorrowRepository;
import com.lms.repository.MemberAccountRepository;
import com.lms.repository.MemberNotificationRepository;
import com.lms.repository.MemberRepository;
import com.lms.repository.NotificationRepository;
import com.lms.repository.ReservationRepository;
import com.lms.repository.SystemSettingRepository;
import com.lms.repository.TransactionRepository;
import com.lms.repository.WalletRepository;
import com.lms.service.impl.BorrowServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BorrowBankPaymentLifecycleTest {
    @Mock MemberRepository memberRepository;
    @Mock BookItemRepository bookItemRepository;
    @Mock BorrowRepository borrowRepository;
    @Mock BorrowDetailRepository borrowDetailRepository;
    @Mock BookRepository bookRepository;
    @Mock MemberAccountRepository memberAccountRepository;
    @Mock SystemSettingRepository systemSettingRepository;
    @Mock ReservationRepository reservationRepository;
    @Mock AuditLogService auditLogService;
    @Mock NotificationRepository notificationRepository;
    @Mock MemberNotificationRepository memberNotificationRepository;
    @Mock WalletRepository walletRepository;
    @Mock TransactionRepository transactionRepository;
    @Mock FinancialService financialService;

    @InjectMocks BorrowServiceImpl service;

    @Test
    void bankCheckoutStaysPendingUntilPaymentIsConfirmed() {
        Member member = new Member();
        member.setMemberId(7);
        User user = new User();
        user.setStatus(UserStatus.Active);
        member.setUser(user);
        Book book = new Book();
        book.setTitle("Clean Code");
        BookItem item = new BookItem();
        item.setBarcode("BC-001");
        item.setBook(book);
        item.setStatus("Available");

        BorrowRequest request = new BorrowRequest();
        request.setMemberIdentifier("0900000000");
        request.setBarcodes(List.of("BC-001"));
        request.setNumberOfDays(14);
        request.setPaymentMethod("BANK");

        when(memberRepository.findByUserEmail("0900000000")).thenReturn(Optional.empty());
        when(memberRepository.findByUserPhone("0900000000")).thenReturn(Optional.of(member));
        when(bookItemRepository.findByBarcodeForUpdate("BC-001")).thenReturn(Optional.of(item));
        when(borrowDetailRepository.countActiveBorrowedBooks(7)).thenReturn(0L);
        when(borrowRepository.save(any(Borrow.class))).thenAnswer(invocation -> {
            Borrow borrow = invocation.getArgument(0);
            borrow.setBorrowId(42);
            return borrow;
        });

        Borrow result = service.processBorrowing(request, "librarian");

        assertThat(result.getStatus()).isEqualTo("Payment_Pending");
        assertThat(item.getStatus()).isEqualTo("Payment_Pending");
        verify(borrowDetailRepository).save(any(BorrowDetail.class));
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void paidBankCheckoutActivatesBorrowAndStartsLoanPeriodAtPaymentTime() {
        Member member = new Member();
        member.setMemberId(7);
        Borrow borrow = new Borrow();
        borrow.setBorrowId(42);
        borrow.setMember(member);
        borrow.setStatus("Payment_Pending");
        borrow.setBorrowDate(LocalDateTime.now().minusMinutes(2));

        Book book = new Book();
        book.setTitle("Clean Code");
        BookItem item = new BookItem();
        item.setBook(book);
        item.setStatus("Payment_Pending");
        BorrowDetail detail = new BorrowDetail();
        detail.setBorrow(borrow);
        detail.setBook(book);
        detail.setBookItem(item);
        detail.setStatus("Payment_Pending");
        detail.setDueDate(borrow.getBorrowDate().plusDays(14));
        LocalDateTime pendingDueDate = detail.getDueDate();

        when(borrowRepository.findById(42)).thenReturn(Optional.of(borrow));
        when(borrowDetailRepository.findByBorrowId(42)).thenReturn(List.of(detail));
        when(borrowRepository.save(any(Borrow.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            notification.setNotificationId(99);
            return notification;
        });

        service.activatePendingBankBorrow(42);

        assertThat(borrow.getStatus()).isEqualTo("Active");
        assertThat(detail.getStatus()).isEqualTo("Borrowed");
        assertThat(item.getStatus()).isEqualTo("Borrowed");
        assertThat(detail.getDueDate()).isAfter(pendingDueDate);
        verify(memberNotificationRepository).save(any());
    }

    @Test
    void cancelledBankCheckoutReleasesHeldBook() {
        Borrow borrow = new Borrow();
        borrow.setBorrowId(42);
        borrow.setStatus("Payment_Pending");
        BookItem item = new BookItem();
        item.setStatus("Payment_Pending");
        BorrowDetail detail = new BorrowDetail();
        detail.setBorrow(borrow);
        detail.setBookItem(item);
        detail.setStatus("Payment_Pending");

        when(borrowRepository.findById(42)).thenReturn(Optional.of(borrow));
        when(borrowDetailRepository.findByBorrowId(42)).thenReturn(List.of(detail));

        service.cancelPendingBankBorrow(42, "CANCELLED");

        assertThat(borrow.getStatus()).isEqualTo("Payment_Cancelled");
        assertThat(detail.getStatus()).isEqualTo("Cancelled");
        assertThat(item.getStatus()).isEqualTo("Available");
    }
}
