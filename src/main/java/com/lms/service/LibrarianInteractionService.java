package com.lms.service;

import com.lms.dto.request.LibrarianReviewModerateRequest;
import com.lms.dto.request.MemberReviewSubmitRequest; // Đảm bảo đã import DTO này
import com.lms.dto.response.LibrarianReviewResponse;
import com.lms.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.lms.dto.request.LibrarianNotificationSendRequest;

import java.util.List;

public interface LibrarianInteractionService {
    // Dành cho Librarian
    Page<LibrarianReviewResponse> getReviewsForModeration(String status, Pageable pageable);
    void moderateReview(Integer feedbackId, LibrarianReviewModerateRequest request);
    // BỔ SUNG DÒNG NÀY VÀO ĐỂ HẾT LỖI @Override
    void submitReview(Integer memberId, MemberReviewSubmitRequest request);
    // Gửi thông báo cho 1 hoặc nhiều member
    void sendNotificationToMembers(LibrarianNotificationSendRequest request);

    List<Member> getAllMembers();
}