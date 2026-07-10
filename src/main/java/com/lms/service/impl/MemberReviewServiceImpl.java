package com.lms.service.impl;

import com.lms.dto.request.MemberReviewSubmitRequest;
import com.lms.entity.MemberAccount;
import com.lms.entity.Book;
import com.lms.entity.Feedback;
import com.lms.entity.Member;
import com.lms.exception.ResourceNotFoundException;
import com.lms.exception.ValidationException;
import com.lms.repository.MemberAccountRepository;
import com.lms.repository.BookRepository;
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

    public MemberReviewServiceImpl(MemberAccountRepository memberAccountRepository,
                                   BookRepository bookRepository,
                                   FeedbackRepository feedbackRepository) {
        this.memberAccountRepository = memberAccountRepository;
        this.bookRepository = bookRepository;
        this.feedbackRepository = feedbackRepository;
    }

    @Override
    @Transactional
    public void submitReview(String username, MemberReviewSubmitRequest request) {
        MemberAccount account = memberAccountRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản: " + username));

        Member member = account.getMember();
        if (member == null) {
            throw new ResourceNotFoundException("Không tìm thấy độc giả với tài khoản: " + username);
        }

        Book book = bookRepository.findById(request.getBookId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sách với ID: " + request.getBookId()));

        if (feedbackRepository.existsByMember_MemberIdAndBook_BookIdAndStatusNot(
                member.getMemberId(), book.getBookId(), DELETED_BY_MEMBER_STATUS)) {
            throw new ValidationException("Bạn đã đánh giá sách này rồi.");
        }

        Feedback feedback = new Feedback();
        feedback.setMember(member);
        feedback.setBook(book);
        feedback.setRating(request.getRating());
        feedback.setComment(request.getComment().trim());
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

        return feedbackRepository.findByMember_MemberIdAndStatusNotOrderByCreatedDateDesc(
                member.getMemberId(), DELETED_BY_MEMBER_STATUS, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Feedback> getApprovedReviewsByBookId(Integer bookId) {
        return feedbackRepository.findByBook_BookIdAndStatusOrderByCreatedDateDesc(bookId, APPROVED_STATUS);
    }

    @Override
    @Transactional
    public void deleteMyReview(String username, Integer feedbackId) {
        MemberAccount account = memberAccountRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản: " + username));

        Member member = account.getMember();
        if (member == null) {
            throw new ResourceNotFoundException("Không tìm thấy độc giả với tài khoản: " + username);
        }

        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đánh giá với ID: " + feedbackId));

        if (feedback.getMember() == null || !member.getMemberId().equals(feedback.getMember().getMemberId())) {
            throw new ValidationException("Bạn không có quyền xoá đánh giá này.");
        }

        feedback.setStatus(DELETED_BY_MEMBER_STATUS);
        feedbackRepository.save(feedback);
    }
}
