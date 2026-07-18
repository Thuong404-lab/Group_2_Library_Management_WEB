package com.lms.service;

import com.lms.entity.*;
import com.lms.repository.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ApprovedBorrowExpiryJobTest {

    @Test
    void testCancelExpiredApprovedBorrows() {
        // Mock repositories
        BorrowRepository borrowRepository = mock(BorrowRepository.class);
        BorrowDetailRepository borrowDetailRepository = mock(BorrowDetailRepository.class);
        BookItemRepository bookItemRepository = mock(BookItemRepository.class);
        NotificationRepository notificationRepository = mock(NotificationRepository.class);
        MemberNotificationRepository memberNotificationRepository = mock(MemberNotificationRepository.class);

        ApprovedBorrowExpiryJob job = new ApprovedBorrowExpiryJob(
                borrowRepository,
                borrowDetailRepository,
                bookItemRepository,
                notificationRepository,
                memberNotificationRepository
        );

        // Prepare test data
        Member member = new Member();
        member.setMemberId(1);

        // 1. A borrow approved 50 hours ago (should be canceled)
        Borrow expiredBorrow = new Borrow();
        expiredBorrow.setBorrowId(101);
        expiredBorrow.setMember(member);
        expiredBorrow.setStatus("Waiting_Pickup");
        expiredBorrow.setBorrowDate(LocalDateTime.now().minusHours(50));

        // 2. A borrow approved 10 hours ago (should NOT be canceled)
        Borrow activeBorrow = new Borrow();
        activeBorrow.setBorrowId(102);
        activeBorrow.setMember(member);
        activeBorrow.setStatus("Waiting_Pickup");
        activeBorrow.setBorrowDate(LocalDateTime.now().minusHours(10));

        // Mock borrowRepository.findAllByStatus("Waiting_Pickup")
        when(borrowRepository.findAllByStatus("Waiting_Pickup")).thenReturn(List.of(expiredBorrow, activeBorrow));

        // Mock borrowDetailRepository.findByBorrowId for expired borrow
        BorrowDetail expiredDetail = new BorrowDetail();
        expiredDetail.setBorrowDetailId(201);
        expiredDetail.setStatus("Waiting_Pickup");
        BookItem item = new BookItem();
        item.setBookItemId(301);
        item.setStatus("Waiting_Pickup");
        expiredDetail.setBookItem(item);
        
        when(borrowDetailRepository.findByBorrowId(101)).thenReturn(List.of(expiredDetail));
        when(borrowDetailRepository.findByBorrowId(102)).thenReturn(List.of());

        // Mock saving notifications
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification n = invocation.getArgument(0);
            n.setNotificationId(999);
            return n;
        });

        // Execute job
        job.cancelExpiredApprovedBorrows();

        // Verify status updates
        assertEquals("Canceled", expiredBorrow.getStatus());
        assertEquals("Canceled", expiredDetail.getStatus());
        assertEquals("Available", item.getStatus());
        verify(borrowRepository).save(expiredBorrow);
        verify(borrowDetailRepository).save(expiredDetail);
        verify(bookItemRepository).save(item);

        // Verify that the active borrow was NOT saved or updated
        verify(borrowRepository, never()).save(activeBorrow);

        // Verify notification was sent
        ArgumentCaptor<Notification> notifCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notifCaptor.capture());
        Notification sentNotif = notifCaptor.getValue();
        assertTrue(sentNotif.getTitle().contains("Vi phạm quy định"));
        assertTrue(sentNotif.getContent().contains("BOR-101"));

        ArgumentCaptor<MemberNotification> memberNotifCaptor = ArgumentCaptor.forClass(MemberNotification.class);
        verify(memberNotificationRepository).save(memberNotifCaptor.capture());
        MemberNotification sentMemberNotif = memberNotifCaptor.getValue();
        assertFalse(sentMemberNotif.getIsRead());
        assertEquals(member, sentMemberNotif.getMember());
    }
}
