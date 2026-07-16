package com.lms.service.impl;

import com.lms.dto.request.MemberReviewSubmitRequest;
import com.lms.dto.request.MemberReviewUpdateRequest;
import com.lms.entity.MemberAccount;
import com.lms.entity.Book;
import com.lms.entity.Feedback;
import com.lms.entity.Member;
import com.lms.exception.ResourceNotFoundException;
import com.lms.exception.ValidationException;
import com.lms.repository.MemberAccountRepository;
import com.lms.repository.BookRepository;
import com.lms.repository.BorrowDetailRepository;
import com.lms.repository.FeedbackRepository;
import com.lms.service.MemberReviewService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MemberReviewServiceImpl implements MemberReviewService {

    private static final String APPROVED_STATUS = "APPROVED";
    private static final String DELETED_BY_MEMBER_STATUS = "DELETED_BY_MEMBER";

    private final MemberAccountRepository memberAccountRepository;
    private final BookRepository bookRepository;
    private final FeedbackRepository feedbackRepository;
    private final BorrowDetailRepository borrowDetailRepository;

    public MemberReviewServiceImpl(MemberAccountRepository memberAccountRepository,
                                   BookRepository bookRepository,
                                   FeedbackRepository feedbackRepository,
                                   BorrowDetailRepository borrowDetailRepository) {
        this.memberAccountRepository = memberAccountRepository;
        this.bookRepository = bookRepository;
        this.feedbackRepository = feedbackRepository;
        this.borrowDetailRepository = borrowDetailRepository;
    }

    @Override
    @Transactional
    public void submitReview(String username, MemberReviewSubmitRequest request) {
        String normalizedComment = validateAndNormalizeComment(request.getComment());
        MemberAccount account = memberAccountRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản: " + username));

        Member member = account.getMember();
        if (member == null) {
            throw new ResourceNotFoundException("Không tìm thấy độc giả với tài khoản: " + username);
        }

        Book book = bookRepository.findById(request.getBookId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sách với ID: " + request.getBookId()));

        if (borrowDetailRepository.countEligibleReviewBorrows(
                member.getMemberId(), book.getBookId()) == 0) {
            throw new ValidationException(
                    "Bạn chỉ có thể đánh giá sách đã từng mượn hoặc đang mượn.");
        }

        if (feedbackRepository.existsByMember_MemberIdAndBook_BookIdAndStatusNot(
                member.getMemberId(), book.getBookId(), DELETED_BY_MEMBER_STATUS)) {
            throw new ValidationException("Bạn đã đánh giá sách này rồi.");
        }

        Feedback feedback = new Feedback();
        feedback.setMember(member);
        feedback.setBook(book);
        feedback.setRating(request.getRating());
        feedback.setComment(normalizedComment);
        feedback.setCreatedDate(LocalDateTime.now());
        feedback.setStatus(APPROVED_STATUS);

        feedbackRepository.save(feedback);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Feedback> getMyReviews(String username) {
        MemberAccount account = memberAccountRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản: " + username));

        Member member = account.getMember();
        if (member == null) {
            throw new ResourceNotFoundException("Không tìm thấy độc giả với tài khoản: " + username);
        }

        return feedbackRepository.findByMember_MemberIdAndStatusNotOrderByCreatedDateDesc(
                member.getMemberId(), DELETED_BY_MEMBER_STATUS);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Feedback> getMyReviews(String username, Pageable pageable) {
        MemberAccount account = memberAccountRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("KhÃ´ng tÃ¬m tháº¥y tÃ i khoáº£n: " + username));

        Member member = account.getMember();
        if (member == null) {
            throw new ResourceNotFoundException("KhÃ´ng tÃ¬m tháº¥y Ä‘á»™c giáº£ vá»›i tÃ i khoáº£n: " + username);
        }

        Page<Feedback> reviews = feedbackRepository.findByMember_MemberIdAndStatusNotOrderByCreatedDateDesc(
                member.getMemberId(), DELETED_BY_MEMBER_STATUS, pageable);
        reviews.forEach(review -> {
            if (review.getBook() != null && review.getBook().getAuthors() != null) {
                review.getBook().getAuthors().size();
            }
        });
        return reviews;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Feedback> getApprovedReviewsByBookId(Integer bookId) {
        return feedbackRepository.findByBook_BookIdAndStatusOrderByCreatedDateDesc(bookId, APPROVED_STATUS);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isEligibleToReview(String username, Integer bookId) {
        Member member = getMemberByUsername(username);
        return borrowDetailRepository.countEligibleReviewBorrows(member.getMemberId(), bookId) > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasActiveReview(String username, Integer bookId) {
        Member member = getMemberByUsername(username);
        return feedbackRepository.existsByMember_MemberIdAndBook_BookIdAndStatusNot(
                member.getMemberId(), bookId, DELETED_BY_MEMBER_STATUS);
    }

    @Override
    @Transactional(readOnly = true)
    public MemberReviewUpdateRequest getMyReviewForEdit(String username, Integer feedbackId) {
        Feedback feedback = getEditableReview(username, feedbackId);
        MemberReviewUpdateRequest request = new MemberReviewUpdateRequest();
        request.setRating(feedback.getRating());
        request.setComment(feedback.getComment());
        return request;
    }

    @Override
    @Transactional
    public void updateMyReview(String username, Integer feedbackId, MemberReviewUpdateRequest request) {
        Feedback feedback = getEditableReview(username, feedbackId);
        String normalizedComment = validateAndNormalizeComment(request.getComment());

        if (borrowDetailRepository.countEligibleReviewBorrows(
                feedback.getMember().getMemberId(), feedback.getBook().getBookId()) == 0) {
            throw new ValidationException(
                    "Bạn chỉ có thể chỉnh sửa đánh giá cho sách đã từng mượn hoặc đang mượn.");
        }

        feedback.setRating(request.getRating());
        feedback.setComment(normalizedComment);
        feedbackRepository.save(feedback);
    }

    private String validateAndNormalizeComment(String comment) {
        String normalizedComment = comment == null ? "" : comment.strip()
                .replaceAll("(?:\\R\\s*){3,}", System.lineSeparator() + System.lineSeparator());
        if (normalizedComment.isEmpty()) {
            throw new ValidationException("Nội dung đánh giá không được để trống.");
        }
        if (normalizedComment.length() < 5 || normalizedComment.length() > 1000) {
            throw new ValidationException("Nội dung đánh giá phải có từ 5 đến 1000 ký tự.");
        }
        if (normalizedComment.codePoints().noneMatch(Character::isLetter)) {
            throw new ValidationException("Nội dung đánh giá không được chỉ gồm số hoặc ký tự đặc biệt.");
        }
        return normalizedComment;
    }

    @Override
    @Transactional
    public void deleteMyReview(String username, Integer feedbackId) {
        Member member = getMemberByUsername(username);

        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đánh giá với ID: " + feedbackId));

        if (feedback.getMember() == null || !member.getMemberId().equals(feedback.getMember().getMemberId())) {
            throw new ValidationException("Bạn không có quyền xoá đánh giá này.");
        }

        feedback.setStatus(DELETED_BY_MEMBER_STATUS);
        feedbackRepository.save(feedback);
    }

    private Member getMemberByUsername(String username) {
        MemberAccount account = memberAccountRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản: " + username));

        Member member = account.getMember();
        if (member == null) {
            throw new ResourceNotFoundException("Không tìm thấy độc giả với tài khoản: " + username);
        }
        return member;
    }

    private Feedback getEditableReview(String username, Integer feedbackId) {
        Member member = getMemberByUsername(username);
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy đánh giá với ID: " + feedbackId));

        if (feedback.getMember() == null
                || !member.getMemberId().equals(feedback.getMember().getMemberId())) {
            throw new ValidationException("Bạn không có quyền chỉnh sửa đánh giá này.");
        }

        if (feedback.getLibrarianResponse() != null
                && !feedback.getLibrarianResponse().isBlank()) {
            throw new ValidationException(
                    "Không thể chỉnh sửa đánh giá đã được thủ thư phản hồi.");
        }

        if (DELETED_BY_MEMBER_STATUS.equals(feedback.getStatus())) {
            throw new ValidationException("Không thể chỉnh sửa đánh giá đã xóa.");
        }
        return feedback;
    }
}
