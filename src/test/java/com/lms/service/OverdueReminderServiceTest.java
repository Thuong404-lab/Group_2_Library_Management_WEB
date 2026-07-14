package com.lms.service;

import com.lms.entity.Book;
import com.lms.entity.Borrow;
import com.lms.entity.BorrowDetail;
import com.lms.entity.Member;
import com.lms.entity.MemberNotification;
import com.lms.entity.Notification;
import com.lms.repository.BorrowDetailRepository;
import com.lms.repository.MemberNotificationRepository;
import com.lms.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OverdueReminderServiceTest {

    @Test
    void sendsUnreadReminderWithoutCreatingAFine() {
        BorrowDetailRepository borrowDetailRepository = mock(BorrowDetailRepository.class);
        NotificationRepository notificationRepository = mock(NotificationRepository.class);
        MemberNotificationRepository memberNotificationRepository = mock(MemberNotificationRepository.class);
        OverdueReminderService service = new OverdueReminderService(
                borrowDetailRepository,
                notificationRepository,
                memberNotificationRepository);

        Member member = mock(Member.class);
        when(member.getMemberId()).thenReturn(7);

        Borrow borrow = mock(Borrow.class);
        when(borrow.getMember()).thenReturn(member);

        Book book = mock(Book.class);
        when(book.getTitle()).thenReturn("Sapiens");

        BorrowDetail detail = mock(BorrowDetail.class);
        when(detail.getBorrowDetailId()).thenReturn(11);
        when(detail.getBorrow()).thenReturn(borrow);
        when(detail.getBook()).thenReturn(book);
        when(detail.getDueDate()).thenReturn(LocalDate.now().minusDays(4).atTime(17, 0));
        when(detail.getReturnDate()).thenReturn(null);
        when(detail.getStatus()).thenReturn("Overdue");

        when(borrowDetailRepository.findById(11)).thenReturn(Optional.of(detail));
        when(memberNotificationRepository
                .findByMemberMemberIdAndNotificationContentContainingIgnoreCaseOrderByNotificationCreatedDateDesc(
                        eq(7),
                        contains("#11")))
                .thenReturn(List.of());
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            notification.setNotificationId(99);
            return notification;
        });

        service.sendReturnReminder(11);

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());
        Notification savedNotification = notificationCaptor.getValue();
        assertTrue(savedNotification.getContent().contains("Sapiens"));
        assertTrue(savedNotification.getContent().contains("4 ngày"));
        assertTrue(savedNotification.getContent().contains("Mã lượt mượn: #11"));

        ArgumentCaptor<MemberNotification> memberNotificationCaptor =
                ArgumentCaptor.forClass(MemberNotification.class);
        verify(memberNotificationRepository).save(memberNotificationCaptor.capture());
        assertFalse(memberNotificationCaptor.getValue().getIsRead());
    }
}
