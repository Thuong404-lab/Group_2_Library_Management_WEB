package com.lms.service;

import com.lms.dto.request.MemberReviewSubmitRequest;
import com.lms.entity.Feedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface MemberReviewService {

    void submitReview(String username, MemberReviewSubmitRequest request);

    List<Feedback> getMyReviews(String username);

    Page<Feedback> getMyReviews(String username, Pageable pageable);

    List<Feedback> getApprovedReviewsByBookId(Integer bookId);

    void deleteMyReview(String username, Integer feedbackId);
}
