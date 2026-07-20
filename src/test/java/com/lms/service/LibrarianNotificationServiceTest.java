package com.lms.service;

import com.lms.dto.request.LibrarianNotificationSendRequest;
import com.lms.dto.request.LibrarianReviewReplyRequest;
import com.lms.entity.Book;
import com.lms.entity.Feedback;
import com.lms.entity.Member;
import com.lms.entity.Notification;
import com.lms.entity.Staff;
import com.lms.entity.StaffAccount;
import com.lms.enums.NotificationRecipientType;
import com.lms.enums.NotificationType;
import com.lms.exception.ValidationException;
import com.lms.repository.BookAcquisitionRequestRepository;
import com.lms.repository.FeedbackRepository;
import com.lms.repository.MemberNotificationRepository;
import com.lms.repository.MemberRepository;
import com.lms.repository.NotificationRepository;
import com.lms.repository.StaffAccountRepository;
import com.lms.service.impl.LibrarianInteractionServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LibrarianNotificationServiceTest {

    @Test
    void editsExistingLibrarianReply() {
        FeedbackRepository feedbackRepository = mock(FeedbackRepository.class);
        NotificationRepository notificationRepository = mock(NotificationRepository.class);
        MemberNotificationRepository memberNotificationRepository = mock(MemberNotificationRepository.class);
        LibrarianInteractionServiceImpl service = new LibrarianInteractionServiceImpl(
                feedbackRepository,
                mock(MemberRepository.class),
                notificationRepository,
                memberNotificationRepository,
                mock(BookAcquisitionRequestRepository.class),
                mock(StaffAccountRepository.class));

        Member member = new Member();
        member.setMemberId(7);
        Book book = new Book();
        book.setTitle("Dế Mèn phiêu lưu ký");
        Feedback feedback = new Feedback();
        feedback.setFeedbackId(21);
        feedback.setMember(member);
        feedback.setBook(book);
        feedback.setStatus("APPROVED");
        feedback.setLibrarianResponse("Phản hồi cũ");

        when(feedbackRepository.findById(21)).thenReturn(Optional.of(feedback));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            notification.setNotificationId(11);
            return notification;
        });

        boolean edited = service.replyReview(21,
                new LibrarianReviewReplyRequest("  Phản hồi sau khi chỉnh sửa.  "));

        assertThat(edited).isTrue();
        assertThat(feedback.getLibrarianResponse()).isEqualTo("Phản hồi sau khi chỉnh sửa.");
        assertThat(feedback.getResponseDate()).isNotNull();
        verify(feedbackRepository).save(feedback);
        verify(memberNotificationRepository).save(any());
    }

    @Test
    void savesSelectedTypeAndAuthenticatedLibrarianAsSender() {
        MemberRepository memberRepository = mock(MemberRepository.class);
        NotificationRepository notificationRepository = mock(NotificationRepository.class);
        MemberNotificationRepository memberNotificationRepository = mock(MemberNotificationRepository.class);
        StaffAccountRepository staffAccountRepository = mock(StaffAccountRepository.class);
        LibrarianInteractionServiceImpl service = service(
                memberRepository, notificationRepository, memberNotificationRepository, staffAccountRepository);

        Staff sender = new Staff();
        sender.setStaffId(3);
        StaffAccount account = new StaffAccount();
        account.setStaff(sender);
        Member member = new Member();
        member.setMemberId(7);

        LibrarianNotificationSendRequest request = new LibrarianNotificationSendRequest();
        request.setRecipientType(NotificationRecipientType.ALL);
        request.setNotificationType(NotificationType.MAINTENANCE);
        request.setTitle("  Bảo trì hệ thống  ");
        request.setContent("  Hệ thống tạm ngưng trong 30 phút.  ");

        when(staffAccountRepository.findByUsername("librarian")).thenReturn(Optional.of(account));
        when(memberRepository.findAllWithActiveAccount()).thenReturn(List.of(member));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            notification.setNotificationId(11);
            return notification;
        });

        service.sendNotificationToMembers(request, "librarian");

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());
        Notification saved = notificationCaptor.getValue();
        assertThat(saved.getNotificationType()).isEqualTo(NotificationType.MAINTENANCE);
        assertThat(saved.getStaff()).isSameAs(sender);
        assertThat(saved.getTitle()).isEqualTo("Bảo trì hệ thống");
        assertThat(saved.getContent()).isEqualTo("Hệ thống tạm ngưng trong 30 phút.");
        verify(memberRepository).findAllWithActiveAccount();
        verify(memberRepository, never()).findAll();
        verify(memberNotificationRepository).saveAll(any());
    }

    @Test
    void rejectsSystemBusinessTypeForManualNotification() {
        MemberRepository memberRepository = mock(MemberRepository.class);
        NotificationRepository notificationRepository = mock(NotificationRepository.class);
        LibrarianInteractionServiceImpl service = service(
                memberRepository,
                notificationRepository,
                mock(MemberNotificationRepository.class),
                mock(StaffAccountRepository.class));
        LibrarianNotificationSendRequest request = validRequest();
        request.setNotificationType(NotificationType.LOAN);

        assertThatThrownBy(() -> service.sendNotificationToMembers(request, "librarian"))
                .isInstanceOf(ValidationException.class);

        verify(notificationRepository, never()).save(any());
        verify(memberRepository, never()).findAllWithActiveAccount();
    }

    @Test
    void rejectsMissingNotificationType() {
        LibrarianInteractionServiceImpl service = service(
                mock(MemberRepository.class),
                mock(NotificationRepository.class),
                mock(MemberNotificationRepository.class),
                mock(StaffAccountRepository.class));
        LibrarianNotificationSendRequest request = new LibrarianNotificationSendRequest();
        request.setRecipientType(NotificationRecipientType.ALL);
        request.setTitle("Thông báo");
        request.setContent("Nội dung");

        assertThatThrownBy(() -> service.sendNotificationToMembers(request, "librarian"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("loại thông báo");
    }

    @Test
    void rejectsContentIdenticalToTitle() {
        LibrarianInteractionServiceImpl service = service(
                mock(MemberRepository.class), mock(NotificationRepository.class),
                mock(MemberNotificationRepository.class), mock(StaffAccountRepository.class));
        LibrarianNotificationSendRequest request = validRequest();
        request.setTitle("Thông báo nghỉ lễ");
        request.setContent("  thông báo nghỉ lễ  ");

        assertThatThrownBy(() -> service.sendNotificationToMembers(request, "librarian"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("không được giống hoàn toàn tiêu đề");
    }

    @Test
    void rejectsContentLongerThanTwoThousandCharacters() {
        LibrarianInteractionServiceImpl service = service(
                mock(MemberRepository.class), mock(NotificationRepository.class),
                mock(MemberNotificationRepository.class), mock(StaffAccountRepository.class));
        LibrarianNotificationSendRequest request = validRequest();
        request.setContent("N".repeat(2001));

        assertThatThrownBy(() -> service.sendNotificationToMembers(request, "librarian"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("10 đến 2000 ký tự");
    }

    private LibrarianNotificationSendRequest validRequest() {
        LibrarianNotificationSendRequest request = new LibrarianNotificationSendRequest();
        request.setRecipientType(NotificationRecipientType.ALL);
        request.setNotificationType(NotificationType.GENERAL);
        request.setTitle("Thông báo thư viện");
        request.setContent("Thư viện cập nhật thông tin mới đến bạn đọc.");
        return request;
    }

    private LibrarianInteractionServiceImpl service(MemberRepository memberRepository,
                                                     NotificationRepository notificationRepository,
                                                     MemberNotificationRepository memberNotificationRepository,
                                                     StaffAccountRepository staffAccountRepository) {
        return new LibrarianInteractionServiceImpl(
                mock(FeedbackRepository.class),
                memberRepository,
                notificationRepository,
                memberNotificationRepository,
                mock(BookAcquisitionRequestRepository.class),
                staffAccountRepository);
    }
}
