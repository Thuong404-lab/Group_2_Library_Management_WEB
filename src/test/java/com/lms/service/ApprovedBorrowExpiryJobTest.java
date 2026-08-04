package com.lms.service;

import com.lms.entity.Book;
import com.lms.entity.BookItem;
import com.lms.entity.Borrow;
import com.lms.entity.BorrowDetail;
import com.lms.entity.Member;
import com.lms.entity.Reservation;
import com.lms.repository.BookItemRepository;
import com.lms.repository.BorrowDetailRepository;
import com.lms.repository.BorrowRepository;
import com.lms.repository.MemberNotificationRepository;
import com.lms.repository.NotificationRepository;
import com.lms.repository.ReservationRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApprovedBorrowExpiryJobTest {

    @Test
    void expiryReleasesCopyAndClosesLinkedReservation() {
        BorrowRepository borrowRepository = mock(BorrowRepository.class);
        BorrowDetailRepository detailRepository = mock(BorrowDetailRepository.class);
        BookItemRepository itemRepository = mock(BookItemRepository.class);
        NotificationRepository notificationRepository = mock(NotificationRepository.class);
        MemberNotificationRepository memberNotificationRepository = mock(MemberNotificationRepository.class);
        ReservationRepository reservationRepository = mock(ReservationRepository.class);
        LocalizedMessageService messages = mock(LocalizedMessageService.class);

        Member member = new Member();
        member.setMemberId(7);
        Book book = new Book();
        book.setBookId(11);
        BookItem item = new BookItem();
        item.setStatus("Waiting_Pickup");

        Borrow borrow = new Borrow();
        borrow.setBorrowId(21);
        borrow.setMember(member);
        borrow.setBorrowDate(LocalDateTime.now().minusHours(49));
        borrow.setStatus("Waiting_Pickup");

        BorrowDetail detail = new BorrowDetail();
        detail.setBorrow(borrow);
        detail.setBook(book);
        detail.setBookItem(item);
        detail.setStatus("Waiting_Pickup");

        Reservation reservation = new Reservation();
        reservation.setMember(member);
        reservation.setBook(book);
        reservation.setStatus("Active");

        when(borrowRepository.findAllByStatus("Waiting_Pickup")).thenReturn(List.of(borrow));
        when(detailRepository.findByBorrowId(21)).thenReturn(List.of(detail));
        when(reservationRepository.findByMemberMemberIdAndStatusInOrderByReservationDateDesc(
                7, List.of("Active"))).thenReturn(List.of(reservation));
        when(notificationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ApprovedBorrowExpiryJob job = new ApprovedBorrowExpiryJob(
                borrowRepository, detailRepository, itemRepository,
                notificationRepository, memberNotificationRepository,
                reservationRepository, messages);

        job.cancelExpiredApprovedBorrows();

        assertThat(borrow.getStatus()).isEqualTo("Canceled");
        assertThat(detail.getStatus()).isEqualTo("Canceled");
        assertThat(item.getStatus()).isEqualTo("Available");
        assertThat(reservation.getStatus()).isEqualTo("Canceled");
        verify(reservationRepository).saveAll(List.of(reservation));
    }
}
