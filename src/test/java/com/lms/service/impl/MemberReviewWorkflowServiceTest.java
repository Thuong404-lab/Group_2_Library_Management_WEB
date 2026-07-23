package com.lms.service.impl;

import com.lms.dto.request.MemberReviewSubmitRequest;
import com.lms.dto.request.MemberReviewUpdateRequest;
import com.lms.entity.Book;
import com.lms.entity.Feedback;
import com.lms.entity.Member;
import com.lms.entity.MemberAccount;
import com.lms.enums.FeedbackStatus;
import com.lms.repository.BookRepository;
import com.lms.repository.BorrowDetailRepository;
import com.lms.repository.FeedbackRepository;
import com.lms.repository.MemberAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberReviewWorkflowServiceTest {

    @Mock private MemberAccountRepository memberAccountRepository;
    @Mock private BookRepository bookRepository;
    @Mock private FeedbackRepository feedbackRepository;
    @Mock private BorrowDetailRepository borrowDetailRepository;

    private MemberReviewServiceImpl service;
    private Member member;
    private Book book;

    @BeforeEach
    void setUp() {
        service = new MemberReviewServiceImpl(memberAccountRepository, bookRepository,
                feedbackRepository, borrowDetailRepository);
        member = new Member();
        member.setMemberId(4);
        book = new Book();
        book.setBookId(8);
    }

    @Test
    void newlySubmittedReviewWaitsForModeration() {
        MemberAccount account = new MemberAccount();
        account.setMember(member);
        when(memberAccountRepository.findByUsername("reader")).thenReturn(Optional.of(account));
        when(bookRepository.findById(8)).thenReturn(Optional.of(book));
        when(borrowDetailRepository.countEligibleReviewBorrows(4, 8)).thenReturn(1L);
        when(feedbackRepository.existsByMember_MemberIdAndBook_BookIdAndStatusNot(
                4, 8, FeedbackStatus.DELETED_BY_MEMBER)).thenReturn(false);

        MemberReviewSubmitRequest request = new MemberReviewSubmitRequest();
        request.setBookId(8);
        request.setRating(5);
        request.setComment("  Rất hữu ích  ");
        service.submitReview("reader", request);

        ArgumentCaptor<Feedback> captor = ArgumentCaptor.forClass(Feedback.class);
        verify(feedbackRepository).saveAndFlush(captor.capture());
        assertEquals(FeedbackStatus.PENDING, captor.getValue().getStatus());
        assertEquals("Rất hữu ích", captor.getValue().getComment());
    }

    @Test
    void editingRejectedReviewResubmitsItForModeration() {
        MemberAccount account = new MemberAccount();
        account.setMember(member);
        Feedback rejected = new Feedback();
        rejected.setFeedbackId(3);
        rejected.setMember(member);
        rejected.setBook(book);
        rejected.setStatus(FeedbackStatus.REJECTED);
        rejected.setModerationReason("Needs revision");
        when(memberAccountRepository.findByUsername("reader")).thenReturn(Optional.of(account));
        when(feedbackRepository.findById(3)).thenReturn(Optional.of(rejected));
        when(borrowDetailRepository.countEligibleReviewBorrows(4, 8)).thenReturn(1L);

        MemberReviewUpdateRequest request = new MemberReviewUpdateRequest();
        request.setRating(4);
        request.setComment("Nội dung đã chỉnh sửa");
        service.updateMyReview("reader", 3, request);

        assertEquals(FeedbackStatus.PENDING, rejected.getStatus());
        assertNull(rejected.getModerationReason());
        assertNull(rejected.getModeratedDate());
        verify(feedbackRepository).save(rejected);
    }
}
