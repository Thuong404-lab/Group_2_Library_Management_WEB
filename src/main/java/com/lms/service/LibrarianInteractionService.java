package com.lms.service;

import com.lms.dto.request.LibrarianNotificationSendRequest;
import com.lms.dto.request.LibrarianReviewReplyRequest;
import com.lms.dto.response.LibrarianReviewResponse;
import com.lms.dto.response.LibrarianNotificationHistoryResponse;
import com.lms.dto.response.NotificationRecipientSearchResponse;
import com.lms.dto.response.NotificationSendResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.lms.entity.BookAcquisitionRequest;
import com.lms.entity.Member;

import java.util.List;

public interface LibrarianInteractionService {

    Page<LibrarianReviewResponse> getReviewsForModeration(String status, Pageable pageable);

    boolean replyReview(Integer feedbackId, LibrarianReviewReplyRequest request);

    void approveReview(Integer feedbackId);

    void rejectReview(Integer feedbackId, String reason);

    void deleteReview(Integer feedbackId);

    NotificationSendResult sendNotificationToMembers(LibrarianNotificationSendRequest request, String senderUsername);

    long countActiveMembers();

    Page<NotificationRecipientSearchResponse> searchNotificationRecipients(String query, Pageable pageable);

    List<NotificationRecipientSearchResponse> getNotificationRecipients(List<Integer> memberIds);

    List<LibrarianNotificationHistoryResponse> getRecentManualNotifications();

    List<Member> getAllMembers();

    Page<BookAcquisitionRequest> getBookAcquisitionRequests(Pageable pageable);

    Page<BookAcquisitionRequest> getBookAcquisitionRequests(String status, String keyword, Pageable pageable);

    void approveBookAcquisitionRequest(Integer requestId, String note, String staffUsername);

    void rejectBookAcquisitionRequest(Integer requestId, String reason, String staffUsername);


}
