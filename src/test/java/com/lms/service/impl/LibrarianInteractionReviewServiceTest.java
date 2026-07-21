package com.lms.service.impl;

import com.lms.dto.request.LibrarianReviewReplyRequest;
import com.lms.dto.response.LibrarianReviewResponse;
import com.lms.entity.Book;
import com.lms.entity.Feedback;
import com.lms.entity.Member;
import com.lms.entity.Notification;
import com.lms.entity.User;
import com.lms.enums.FeedbackStatus;
import com.lms.exception.ConflictException;
import com.lms.repository.BookAcquisitionRequestRepository;
import com.lms.repository.FeedbackRepository;
import com.lms.repository.MemberNotificationRepository;
import com.lms.repository.MemberRepository;
import com.lms.repository.NotificationRepository;
import com.lms.repository.StaffAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LibrarianInteractionReviewServiceTest {

    @Mock private FeedbackRepository feedbackRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private MemberNotificationRepository memberNotificationRepository;
    @Mock private BookAcquisitionRequestRepository acquisitionRequestRepository;
    @Mock private StaffAccountRepository staffAccountRepository;

    private LibrarianInteractionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new LibrarianInteractionServiceImpl(feedbackRepository, memberRepository,
                notificationRepository, memberNotificationRepository, acquisitionRequestRepository,
                staffAccountRepository);
    }

    @Test
    void filtersReviewsByValidatedStatus() {
        Feedback pending = review(FeedbackStatus.PENDING);
        PageRequest pageable = PageRequest.of(0, 10);
        when(feedbackRepository.findByStatus(FeedbackStatus.PENDING, pageable))
                .thenReturn(new PageImpl<>(List.of(pending), pageable, 1));

        List<LibrarianReviewResponse> result = service
                .getReviewsForModeration(" pending ", pageable).getContent();

        assertEquals(1, result.size());
        assertEquals("PENDING", result.get(0).getStatus());
        verify(feedbackRepository, never()).findAll(pageable);
    }

    @Test
    void approvingPendingReviewTransitionsAndNotifiesMember() {
        Feedback pending = review(FeedbackStatus.PENDING);
        when(feedbackRepository.findById(1)).thenReturn(Optional.of(pending));
        stubNotificationSave();

        service.approveReview(1);

        assertEquals(FeedbackStatus.APPROVED, pending.getStatus());
        assertNull(pending.getModerationReason());
        verify(feedbackRepository).save(pending);
        verify(memberNotificationRepository).save(any());
    }

    @Test
    void replyIsRejectedUntilReviewIsApproved() {
        Feedback pending = review(FeedbackStatus.PENDING);
        when(feedbackRepository.findById(1)).thenReturn(Optional.of(pending));

        assertThrows(ConflictException.class,
                () -> service.replyReview(1, new LibrarianReviewReplyRequest("Thank you")));

        verify(feedbackRepository, never()).save(any());
    }

    @Test
    void rejectingReviewKeepsRecordAndClearsOldReply() {
        Feedback approved = review(FeedbackStatus.APPROVED);
        approved.setLibrarianResponse("Old response");
        approved.setResponseDate(LocalDateTime.now());
        when(feedbackRepository.findById(1)).thenReturn(Optional.of(approved));
        stubNotificationSave();

        service.rejectReview(1, "  Nội dung chưa phù hợp  ");

        assertEquals(FeedbackStatus.REJECTED, approved.getStatus());
        assertEquals("Nội dung chưa phù hợp", approved.getModerationReason());
        assertNull(approved.getLibrarianResponse());
        assertNull(approved.getResponseDate());
        verify(feedbackRepository).save(approved);
        verify(feedbackRepository, never()).delete(any());
    }

    private Feedback review(FeedbackStatus status) {
        User user = new User();
        user.setFullName("Reader");
        Member member = new Member();
        member.setMemberId(7);
        member.setUser(user);
        Book book = new Book();
        book.setBookId(11);
        book.setTitle("Clean Code");
        Feedback feedback = new Feedback();
        feedback.setFeedbackId(1);
        feedback.setMember(member);
        feedback.setBook(book);
        feedback.setRating(5);
        feedback.setComment("Useful book");
        feedback.setCreatedDate(LocalDateTime.now());
        feedback.setStatus(status);
        return feedback;
    }

    private void stubNotificationSave() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            notification.setNotificationId(99);
            return notification;
        });
    }
}
