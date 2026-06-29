package com.lms.service.impl;

import com.lms.dto.request.MemberReviewSubmitRequest;
import com.lms.entity.Account;
import com.lms.entity.Book;
import com.lms.entity.Feedback;
import com.lms.entity.Member;
import com.lms.exception.ResourceNotFoundException;
import com.lms.exception.ValidationException;
import com.lms.repository.AccountRepository;
import com.lms.repository.BookRepository;
import com.lms.repository.FeedbackRepository;
import com.lms.repository.MemberRepository;
import com.lms.service.MemberReviewService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class MemberReviewServiceImpl implements MemberReviewService {

    private final AccountRepository accountRepository;
    private final MemberRepository memberRepository;
    private final BookRepository bookRepository;
    private final FeedbackRepository feedbackRepository;

    public MemberReviewServiceImpl(AccountRepository accountRepository,
                                   MemberRepository memberRepository,
                                   BookRepository bookRepository,
                                   FeedbackRepository feedbackRepository) {
        this.accountRepository = accountRepository;
        this.memberRepository = memberRepository;
        this.bookRepository = bookRepository;
        this.feedbackRepository = feedbackRepository;
    }

    @Override
    @Transactional
    public void submitReview(String username, MemberReviewSubmitRequest request) {
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản: " + username));

        Member member = memberRepository.findByUserId(account.getUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy độc giả với tài khoản: " + username));

        Book book = bookRepository.findById(request.getBookId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sách với ID: " + request.getBookId()));

        if (feedbackRepository.existsByMember_MemberIdAndBook_BookId(member.getMemberId(), book.getBookId())) {
            throw new ValidationException("Bạn đã đánh giá sách này rồi.");
        }

        Feedback feedback = new Feedback();
        feedback.setMember(member);
        feedback.setBook(book);
        feedback.setRating(request.getRating());
        feedback.setComment(request.getComment().trim());
        feedback.setCreatedDate(LocalDateTime.now());
        feedback.setStatus("APPROVED");

        feedbackRepository.save(feedback);
    }
}