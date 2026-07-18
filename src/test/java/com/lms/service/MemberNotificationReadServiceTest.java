package com.lms.service;

import com.lms.entity.Member;
import com.lms.entity.MemberAccount;
import com.lms.entity.MemberNotification;
import com.lms.entity.MemberNotificationId;
import com.lms.entity.Notification;
import com.lms.enums.NotificationEventType;
import com.lms.enums.NotificationSource;
import com.lms.enums.NotificationType;
import com.lms.repository.MemberAccountRepository;
import com.lms.repository.MemberNotificationRepository;
import com.lms.repository.NotificationRepository;
import com.lms.service.impl.MemberNotificationServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class MemberNotificationReadServiceTest {

    private MemberAccountRepository accountRepository;
    private MemberNotificationRepository memberNotificationRepository;
    private NotificationRepository notificationRepository;
    private MemberNotificationServiceImpl service;
    private Member member;

    @BeforeEach
    void setUp() {
        LocaleContextHolder.setLocale(Locale.forLanguageTag("vi"));
        accountRepository = mock(MemberAccountRepository.class);
        memberNotificationRepository = mock(MemberNotificationRepository.class);
        notificationRepository = mock(NotificationRepository.class);
        service = new MemberNotificationServiceImpl(
                accountRepository, memberNotificationRepository, notificationRepository);

        member = new Member();
        member.setMemberId(7);
        MemberAccount account = new MemberAccount();
        account.setMember(member);
        when(accountRepository.findByUsername("member7")).thenReturn(Optional.of(account));
    }

    @AfterEach
    void resetLocale() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void marksOnlySelectedNotificationAndReturnsRemainingUnreadCount() {
        MemberNotification selected = new MemberNotification();
        selected.setIsRead(false);
        MemberNotificationId selectedId = new MemberNotificationId(7, 21);

        when(memberNotificationRepository.findById(selectedId)).thenReturn(Optional.of(selected));
        when(memberNotificationRepository.countByMember_MemberIdAndIsReadFalse(7)).thenReturn(12L);

        long unreadCount = service.markNotificationAsRead("member7", 21);

        assertThat(selected.getIsRead()).isTrue();
        assertThat(selected.getReadDate()).isNotNull();
        assertThat(unreadCount).isEqualTo(12L);
        verify(memberNotificationRepository).save(selected);
        verify(memberNotificationRepository, never()).markUnreadNotificationsAsRead(anyInt(), any());
    }

    @Test
    void marksAllUnreadNotificationsForOnlyTheCurrentMember() {
        service.markAllNotificationsAsRead("member7");

        verify(memberNotificationRepository).markUnreadNotificationsAsRead(eq(7), any(LocalDateTime.class));
    }

    @Test
    void returnsAPageAndRendersI18nWhilePreservingNotificationSources() {
        Notification librarianNotification = notification(
                21, NotificationSource.LIBRARIAN, NotificationType.REVIEW,
                "notification.reviewReply.title", "notification.reviewReply.content", "Clean Code");
        Notification systemNotification = notification(
                22, NotificationSource.SYSTEM, NotificationType.LOAN,
                "systemNotification.borrow.requested.title",
                "systemNotification.borrow.requested.content", "Clean Architecture");
        var pageable = PageRequest.of(1, 20);
        when(memberNotificationRepository.findNotificationPage(
                7, null, null, pageable))
                .thenReturn(new PageImpl<>(
                        List.of(memberNotification(librarianNotification), memberNotification(systemNotification)),
                        pageable, 42));

        var page = service.getMyNotifications("member7", null, null, pageable);

        assertThat(page.getTotalElements()).isEqualTo(42);
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent().get(0).getTitle()).isEqualTo("Phản hồi đánh giá");
        assertThat(page.getContent().get(0).getContent()).contains("Clean Code");
        assertThat(page.getContent().get(0).getFromLibrarian()).isTrue();
        assertThat(page.getContent().get(1).getTitle()).isEqualTo("Yêu cầu mượn sách thành công");
        assertThat(page.getContent().get(1).getFromLibrarian()).isFalse();
    }

    @Test
    void persistsNotificationBeforeLinkingItToTheMember() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            notification.setNotificationId(31);
            return notification;
        });

        service.sendNotificationToUser("member7", "Library update", "The library opens at 8 AM.");

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());
        Notification saved = notificationCaptor.getValue();
        assertThat(saved.getNotificationSource()).isEqualTo(NotificationSource.SYSTEM);
        assertThat(saved.getEventType()).isEqualTo(NotificationEventType.GENERAL);
        assertThat(saved.getNotificationType()).isEqualTo(NotificationType.GENERAL);

        ArgumentCaptor<MemberNotification> linkCaptor = ArgumentCaptor.forClass(MemberNotification.class);
        verify(memberNotificationRepository).save(linkCaptor.capture());
        assertThat(linkCaptor.getValue().getId()).isEqualTo(new MemberNotificationId(7, 31));
        assertThat(linkCaptor.getValue().getIsRead()).isFalse();
    }

    private Notification notification(Integer id,
                                      NotificationSource source,
                                      NotificationType type,
                                      String titleKey,
                                      String contentKey,
                                      Object... arguments) {
        Notification notification = new Notification();
        notification.setNotificationId(id);
        LocalizedMessageService.fallback().prepareNotification(
                notification, titleKey, contentKey, arguments);
        notification.setNotificationSource(source);
        notification.setNotificationType(type);
        notification.setCreatedDate(LocalDateTime.now());
        return notification;
    }

    private MemberNotification memberNotification(Notification notification) {
        MemberNotification memberNotification = new MemberNotification();
        memberNotification.setId(new MemberNotificationId(7, notification.getNotificationId()));
        memberNotification.setMember(member);
        memberNotification.setNotification(notification);
        memberNotification.setIsRead(false);
        return memberNotification;
    }
}
