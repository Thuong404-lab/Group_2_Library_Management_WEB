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

import java.util.List;

public interface LibrarianInteractionService {

    Page<LibrarianReviewResponse> getReviewsForModeration(String status, Pageable pageable);

    boolean replyReview(Integer feedbackId, LibrarianReviewReplyRequest request);

    void deleteReview(Integer feedbackId);

    NotificationSendResult sendNotificationToMembers(LibrarianNotificationSendRequest request, String senderUsername);

    long countActiveMembers();

    Page<NotificationRecipientSearchResponse> searchNotificationRecipients(String query, Pageable pageable);

    List<NotificationRecipientSearchResponse> getNotificationRecipients(List<Integer> memberIds);

    List<LibrarianNotificationHistoryResponse> getRecentManualNotifications();

    Page<BookAcquisitionRequest> getBookAcquisitionRequests(Pageable pageable);

    void approveBookAcquisitionRequest(Integer requestId);

    void rejectBookAcquisitionRequest(Integer requestId, String reason);


}
