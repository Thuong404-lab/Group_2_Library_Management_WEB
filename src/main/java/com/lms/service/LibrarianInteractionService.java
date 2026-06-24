package com.lms.service;

import com.lms.dto.request.LibrarianReviewModerateRequest;
import com.lms.dto.request.MemberReviewSubmitRequest; // Đảm bảo đã import DTO này
import com.lms.dto.response.LibrarianReviewResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LibrarianInteractionService {
    // Dành cho Librarian
    Page<LibrarianReviewResponse> getReviewsForModeration(String status, Pageable pageable);
    void moderateReview(Integer feedbackId, LibrarianReviewModerateRequest request, Integer staffId);

    // BỔ SUNG DÒNG NÀY VÀO ĐỂ HẾT LỖI @Override
    void submitReview(Integer memberId, MemberReviewSubmitRequest request);
}