package com.lms.service;

import com.lms.dto.request.LibrarianNotificationSendRequest;
import com.lms.dto.request.LibrarianReviewReplyRequest;
import com.lms.dto.response.LibrarianReviewResponse;
import com.lms.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.lms.entity.BookAcquisitionRequest;

import java.util.List;

public interface LibrarianInteractionService {

    Page<LibrarianReviewResponse> getReviewsForModeration(String status, Pageable pageable);

    boolean replyReview(Integer feedbackId, LibrarianReviewReplyRequest request);

    void approveReview(Integer feedbackId);

    void rejectReview(Integer feedbackId, String reason);

    void sendNotificationToMembers(LibrarianNotificationSendRequest request, String senderUsername);

    List<Member> getAllMembers();

    Page<BookAcquisitionRequest> getBookAcquisitionRequests(Pageable pageable);

    void approveBookAcquisitionRequest(Integer requestId);

    void rejectBookAcquisitionRequest(Integer requestId, String reason);


}
