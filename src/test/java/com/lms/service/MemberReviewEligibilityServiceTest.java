package com.lms.service;

import com.lms.dto.request.MemberReviewSubmitRequest;
import com.lms.dto.request.MemberReviewUpdateRequest;
import com.lms.entity.Book;
import com.lms.entity.Feedback;
import com.lms.entity.Member;
import com.lms.entity.MemberAccount;
import com.lms.exception.ValidationException;
import com.lms.exception.ForbiddenException;
import com.lms.repository.BookRepository;
import com.lms.repository.BorrowDetailRepository;
import com.lms.repository.FeedbackRepository;
import com.lms.repository.MemberAccountRepository;
import com.lms.service.impl.MemberReviewServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MemberReviewEligibilityServiceTest {

    private MemberAccountRepository memberAccountRepository;
    private BookRepository bookRepository;
    private FeedbackRepository feedbackRepository;
    private BorrowDetailRepository borrowDetailRepository;
    private MemberReviewServiceImpl service;
    private Member member;
    private Book book;

    @BeforeEach
    void setUp() {
        LocaleContextHolder.setLocale(Locale.forLanguageTag("vi"));
        memberAccountRepository = mock(MemberAccountRepository.class);
        bookRepository = mock(BookRepository.class);
        feedbackRepository = mock(FeedbackRepository.class);
        borrowDetailRepository = mock(BorrowDetailRepository.class);
        service = new MemberReviewServiceImpl(
                memberAccountRepository, bookRepository, feedbackRepository, borrowDetailRepository);

        member = new Member();
        member.setMemberId(7);
        MemberAccount account = new MemberAccount();
        account.setMember(member);
        book = new Book();
        book.setBookId(11);

        when(memberAccountRepository.findByUsername("member7")).thenReturn(Optional.of(account));
        when(bookRepository.findById(11)).thenReturn(Optional.of(book));
    }

    @AfterEach
    void resetLocale() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void allowsReviewWhenMemberHasAnEligibleBorrow() {
        when(borrowDetailRepository.countEligibleReviewBorrows(7, 11)).thenReturn(1L);
        when(feedbackRepository.existsByMember_MemberIdAndBook_BookIdAndStatusNot(
                7, 11, "DELETED_BY_MEMBER")).thenReturn(false);

        service.submitReview("member7", reviewRequest());

        ArgumentCaptor<Feedback> feedbackCaptor = ArgumentCaptor.forClass(Feedback.class);
        verify(feedbackRepository).save(feedbackCaptor.capture());
        assertThat(feedbackCaptor.getValue().getMember()).isSameAs(member);
        assertThat(feedbackCaptor.getValue().getBook()).isSameAs(book);
        assertThat(feedbackCaptor.getValue().getRating()).isEqualTo(5);
    }

    @Test
    void rejectsReviewWhenMemberHasNeverBorrowedTheBook() {
        when(borrowDetailRepository.countEligibleReviewBorrows(7, 11)).thenReturn(0L);

        assertThatThrownBy(() -> service.submitReview("member7", reviewRequest()))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("đã từng mượn hoặc đang mượn");

        verify(feedbackRepository, never()).save(any(Feedback.class));
    }

    @Test
    void rejectsNewReviewContainingOnlyNumbersAndSpecialCharacters() {
        MemberReviewSubmitRequest request = reviewRequest();
        request.setComment("12345!!!");

        assertThatThrownBy(() -> service.submitReview("member7", request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("số hoặc ký tự đặc biệt");
        verify(feedbackRepository, never()).save(any(Feedback.class));
    }

    @Test
    void reportsEligibilityForTheBookDetailPage() {
        when(borrowDetailRepository.countEligibleReviewBorrows(7, 11)).thenReturn(2L);

        assertThat(service.isEligibleToReview("member7", 11)).isTrue();
    }

    @Test
    void updatesOwnReviewBeforeLibrarianResponse() {
        Feedback feedback = review();
        when(feedbackRepository.findById(21)).thenReturn(Optional.of(feedback));
        when(borrowDetailRepository.countEligibleReviewBorrows(7, 11)).thenReturn(1L);
        MemberReviewUpdateRequest request = new MemberReviewUpdateRequest();
        request.setRating(4);
        request.setComment("  Nội dung sau khi chỉnh sửa.  ");

        service.updateMyReview("member7", 21, request);

        assertThat(feedback.getRating()).isEqualTo(4);
        assertThat(feedback.getComment()).isEqualTo("Nội dung sau khi chỉnh sửa.");
        verify(feedbackRepository).save(feedback);
    }

    @Test
    void rejectsEditedReviewShorterThanFiveCharacters() {
        Feedback feedback = review();
        when(feedbackRepository.findById(21)).thenReturn(Optional.of(feedback));
        MemberReviewUpdateRequest request = new MemberReviewUpdateRequest();
        request.setRating(4);
        request.setComment("abc");

        assertThatThrownBy(() -> service.updateMyReview("member7", 21, request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("5 đến 1000 ký tự");
        verify(feedbackRepository, never()).save(feedback);
    }

    @Test
    void rejectsUpdateAfterLibrarianResponse() {
        Feedback feedback = review();
        feedback.setLibrarianResponse("Đã tiếp nhận phản hồi.");
        when(feedbackRepository.findById(21)).thenReturn(Optional.of(feedback));
        MemberReviewUpdateRequest request = new MemberReviewUpdateRequest();
        request.setRating(3);
        request.setComment("Nội dung mới");

        assertThatThrownBy(() -> service.updateMyReview("member7", 21, request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("đã được thủ thư phản hồi");

        verify(feedbackRepository, never()).save(feedback);
    }

    @Test
    void rejectsUpdateOwnedByAnotherMember() {
        Feedback feedback = review();
        Member anotherMember = new Member();
        anotherMember.setMemberId(8);
        feedback.setMember(anotherMember);
        when(feedbackRepository.findById(21)).thenReturn(Optional.of(feedback));
        MemberReviewUpdateRequest request = new MemberReviewUpdateRequest();
        request.setRating(4);
        request.setComment("Nội dung mới");

        assertThatThrownBy(() -> service.updateMyReview("member7", 21, request))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("không có quyền chỉnh sửa");

        verify(feedbackRepository, never()).save(feedback);
    }

    private MemberReviewSubmitRequest reviewRequest() {
        MemberReviewSubmitRequest request = new MemberReviewSubmitRequest();
        request.setBookId(11);
        request.setRating(5);
        request.setComment("Cuốn sách rất hữu ích.");
        return request;
    }

    private Feedback review() {
        Feedback feedback = new Feedback();
        feedback.setFeedbackId(21);
        feedback.setMember(member);
        feedback.setBook(book);
        feedback.setRating(5);
        feedback.setComment("Nội dung ban đầu.");
        feedback.setStatus("APPROVED");
        return feedback;
    }
}
