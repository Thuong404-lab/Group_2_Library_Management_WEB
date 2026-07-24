package com.lms.service.impl;

import com.lms.dto.request.MemberReviewSubmitRequest;
import com.lms.dto.request.MemberReviewUpdateRequest;
import com.lms.util.ReviewPolicy;
import com.lms.entity.MemberAccount;
import com.lms.entity.Book;
import com.lms.entity.Feedback;
import com.lms.entity.Member;
import com.lms.enums.FeedbackStatus;
import com.lms.exception.ResourceNotFoundException;
import com.lms.exception.ConflictException;
import com.lms.exception.ForbiddenException;
import com.lms.exception.ValidationException;
import com.lms.repository.MemberAccountRepository;
import com.lms.repository.BookRepository;
import com.lms.repository.BorrowDetailRepository;
import com.lms.repository.FeedbackRepository;
import com.lms.service.MemberReviewService;
import com.lms.service.LocalizedMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MemberReviewServiceImpl implements MemberReviewService {

    @Autowired
    private LocalizedMessageService messages = LocalizedMessageService.fallback();

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
        validateRating(request.getRating());
        if (request.getBookId() == null) {
            throw new ValidationException(messages.get("validation.review.bookRequired"));
        }
        MemberAccount account = memberAccountRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(messages.get("backend.profile.accountNotFound", username)));

        Member member = account.getMember();
        if (member == null) {
            throw new ResourceNotFoundException(messages.get("backend.review.memberAccountNotFound", username));
        }

        Book book = bookRepository.findById(request.getBookId())
                .orElseThrow(() -> new ResourceNotFoundException(messages.get("backend.inventory.bookNotFound", request.getBookId())));

        if (borrowDetailRepository.countEligibleReviewBorrows(
                member.getMemberId(), book.getBookId()) == 0) {
            throw new ValidationException(
                    messages.get("backend.review.borrowRequired"));
        }

        if (feedbackRepository.existsByMember_MemberIdAndBook_BookIdAndStatusNot(
                member.getMemberId(), book.getBookId(), FeedbackStatus.DELETED_BY_MEMBER)) {
            throw new ConflictException(messages.get("backend.review.alreadySubmitted"));
        }

        Feedback feedback = new Feedback();
        feedback.setMember(member);
        feedback.setBook(book);
        feedback.setRating(request.getRating());
        feedback.setComment(normalizedComment);
        feedback.setCreatedDate(LocalDateTime.now());
        feedback.setStatus(FeedbackStatus.PENDING);

        try {
            feedbackRepository.saveAndFlush(feedback);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException(messages.get("backend.review.alreadySubmitted"), exception);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Feedback> getMyReviews(String username) {
        MemberAccount account = memberAccountRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(messages.get("backend.profile.accountNotFound", username)));

        Member member = account.getMember();
        if (member == null) {
            throw new ResourceNotFoundException(messages.get("backend.review.memberAccountNotFound", username));
        }

        return feedbackRepository.findByMember_MemberIdAndStatusNotOrderByCreatedDateDesc(
                member.getMemberId(), FeedbackStatus.DELETED_BY_MEMBER);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Feedback> getMyReviews(String username, Pageable pageable) {
        MemberAccount account = memberAccountRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(messages.get("backend.profile.accountNotFound", username)));

        Member member = account.getMember();
        if (member == null) {
            throw new ResourceNotFoundException(messages.get("backend.review.memberAccountNotFound", username));
        }

        Page<Feedback> reviews = feedbackRepository.findByMember_MemberIdAndStatusNotOrderByCreatedDateDesc(
                member.getMemberId(), FeedbackStatus.DELETED_BY_MEMBER, pageable);
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
        return feedbackRepository.findByBook_BookIdAndStatusOrderByCreatedDateDesc(bookId, FeedbackStatus.APPROVED);
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
                member.getMemberId(), bookId, FeedbackStatus.DELETED_BY_MEMBER);
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
        validateRating(request.getRating());

        if (borrowDetailRepository.countEligibleReviewBorrows(
                feedback.getMember().getMemberId(), feedback.getBook().getBookId()) == 0) {
            throw new ValidationException(
                    messages.get("backend.review.editBorrowRequired"));
        }

        feedback.setRating(request.getRating());
        feedback.setComment(normalizedComment);
        if (feedback.getStatus() == FeedbackStatus.REJECTED) {
            feedback.setStatus(FeedbackStatus.PENDING);
            feedback.setModerationReason(null);
            feedback.setModeratedDate(null);
        }
        feedbackRepository.save(feedback);
    }

    private String validateAndNormalizeComment(String comment) {
        String normalizedComment = comment == null ? "" : comment.strip()
                .replaceAll("(?:\\R\\s*){3,}", System.lineSeparator() + System.lineSeparator());
        if (normalizedComment.isEmpty()) {
            throw new ValidationException(messages.get("backend.review.contentRequired"));
        }
        if (normalizedComment.length() < ReviewPolicy.CONTENT_MIN_LENGTH
                || normalizedComment.length() > ReviewPolicy.CONTENT_MAX_LENGTH) {
            throw new ValidationException(messages.get("backend.review.contentRange"));
        }
        if (normalizedComment.codePoints().noneMatch(Character::isLetter)) {
            throw new ValidationException(messages.get("backend.review.contentLetters"));
        }
        return normalizedComment;
    }

    private void validateRating(Integer rating) {
        if (rating == null) {
            throw new ValidationException(messages.get("validation.review.ratingRequired"));
        }
        if (rating < 1) {
            throw new ValidationException(messages.get("validation.review.ratingMinimum"));
        }
        if (rating > 5) {
            throw new ValidationException(messages.get("validation.review.ratingMaximum"));
        }
    }

    @Override
    @Transactional
    public void deleteMyReview(String username, Integer feedbackId) {
        Member member = getMemberByUsername(username);

        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new ResourceNotFoundException(messages.get("backend.review.notFound", feedbackId)));

        if (feedback.getMember() == null || !member.getMemberId().equals(feedback.getMember().getMemberId())) {
            throw new ForbiddenException(messages.get("backend.review.deleteForbidden"));
        }

        feedback.setStatus(FeedbackStatus.DELETED_BY_MEMBER);
        feedbackRepository.save(feedback);
    }

    private Member getMemberByUsername(String username) {
        MemberAccount account = memberAccountRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(messages.get("backend.profile.accountNotFound", username)));

        Member member = account.getMember();
        if (member == null) {
            throw new ResourceNotFoundException(messages.get("backend.review.memberAccountNotFound", username));
        }
        return member;
    }

    private Feedback getEditableReview(String username, Integer feedbackId) {
        Member member = getMemberByUsername(username);
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messages.get("backend.review.notFound", feedbackId)));

        if (feedback.getMember() == null
                || !member.getMemberId().equals(feedback.getMember().getMemberId())) {
            throw new ForbiddenException(messages.get("backend.review.editForbidden"));
        }

        if (feedback.getLibrarianResponse() != null
                && !feedback.getLibrarianResponse().isBlank()) {
            throw new ValidationException(
                    messages.get("backend.review.repliedCannotEdit"));
        }

        if (FeedbackStatus.DELETED_BY_MEMBER == feedback.getStatus()) {
            throw new ConflictException(messages.get("backend.review.deletedCannotEdit"));
        }
        return feedback;
    }
}
