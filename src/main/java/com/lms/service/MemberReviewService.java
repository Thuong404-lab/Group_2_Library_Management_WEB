package com.lms.service;

import com.lms.dto.request.MemberReviewSubmitRequest;

public interface MemberReviewService {

    void submitReview(String username, MemberReviewSubmitRequest request);
}