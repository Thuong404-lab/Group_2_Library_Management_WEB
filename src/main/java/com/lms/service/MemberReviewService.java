package com.lms.service;

import com.lms.dto.request.MemberReviewSubmitRequest;
import com.lms.entity.Feedback;

import java.util.List;

public interface MemberReviewService {

    void submitReview(String username, MemberReviewSubmitRequest request);

    List<Feedback> getMyReviews(String username);
}
