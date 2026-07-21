package com.lms.service.impl;

import com.lms.dto.request.LibrarianNotificationSendRequest;
import com.lms.dto.response.NotificationSendResult;
import com.lms.entity.*;
import com.lms.enums.*;
import com.lms.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.StreamSupport;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LibrarianNotificationSendServiceTest {
    private MemberRepository members;
    private NotificationRepository notifications;
    private MemberNotificationRepository deliveries;
    private StaffAccountRepository staffAccounts;
    private LibrarianInteractionServiceImpl service;
    private Staff sender;

    @BeforeEach
    void setUp() {
        members = mock(MemberRepository.class);
        notifications = mock(NotificationRepository.class);
        deliveries = mock(MemberNotificationRepository.class);
        staffAccounts = mock(StaffAccountRepository.class);
        service = new LibrarianInteractionServiceImpl(
                mock(FeedbackRepository.class), members, notifications, deliveries,
                mock(BookAcquisitionRequestRepository.class), staffAccounts);

        sender = new Staff();
        sender.setStaffId(7);
        StaffAccount account = new StaffAccount();
        account.setStaff(sender);
        when(staffAccounts.findByUsernameForNotificationSend("librarian"))
                .thenReturn(Optional.of(account));
    }

    @Test
    void sendsOnceAfterDeduplicatingSelectedMemberIds() {
        LibrarianNotificationSendRequest request = request("123e4567-e89b-12d3-a456-426614174000");
        request.setMemberIds(List.of(1, 1, 2));
        Member first = member(1);
        Member second = member(2);
        when(members.findAllWithActiveAccountByMemberIdIn(List.of(1, 2)))
                .thenReturn(List.of(first, second));
        when(notifications.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification saved = invocation.getArgument(0);
            saved.setNotificationId(25);
            return saved;
        });

        NotificationSendResult result = service.sendNotificationToMembers(request, "librarian");

        assertEquals(25, result.notificationId());
        assertEquals(2, result.recipientCount());
        assertFalse(result.duplicateRequest());
        verify(deliveries).saveAll(argThat(rows ->
                StreamSupport.stream(rows.spliterator(), false).count() == 2));
    }

    @Test
    void returnsOriginalResultWhenRequestTokenWasAlreadyProcessed() {
        LibrarianNotificationSendRequest request = request("123e4567-e89b-12d3-a456-426614174001");
        Notification previous = new Notification();
        previous.setNotificationId(30);
        previous.setStaff(sender);
        previous.setCreatedDate(LocalDateTime.of(2026, 7, 22, 10, 30));
        when(notifications.findByRequestKey(request.getRequestToken())).thenReturn(Optional.of(previous));
        when(deliveries.countByNotification_NotificationId(30)).thenReturn(18L);

        NotificationSendResult result = service.sendNotificationToMembers(request, "librarian");

        assertTrue(result.duplicateRequest());
        assertEquals(30, result.notificationId());
        assertEquals(18, result.recipientCount());
        verify(notifications, never()).save(any());
        verify(deliveries, never()).saveAll(any());
    }

    private LibrarianNotificationSendRequest request(String token) {
        LibrarianNotificationSendRequest request = new LibrarianNotificationSendRequest();
        request.setRecipientType(NotificationRecipientType.SELECTED);
        request.setNotificationType(NotificationType.GENERAL);
        request.setMemberIds(List.of(1));
        request.setTitle("Thông báo mới");
        request.setContent("Nội dung thông báo hợp lệ");
        request.setRequestToken(token);
        return request;
    }

    private Member member(int id) {
        Member member = new Member();
        member.setMemberId(id);
        return member;
    }
}
