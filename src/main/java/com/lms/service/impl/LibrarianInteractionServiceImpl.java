package com.lms.service.impl;

import com.lms.dto.request.LibrarianNotificationSendRequest;
import com.lms.dto.request.LibrarianReviewReplyRequest;
import com.lms.dto.response.LibrarianReviewResponse;
import com.lms.entity.*;
import com.lms.exception.ResourceNotFoundException;
import com.lms.exception.ValidationException;
import com.lms.repository.*;
import com.lms.service.LibrarianInteractionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class LibrarianInteractionServiceImpl implements LibrarianInteractionService {

    private static final String DELETED_BY_MEMBER_STATUS = "DELETED_BY_MEMBER";

    private final FeedbackRepository feedbackRepository;
    private final MemberRepository memberRepository;
    private final NotificationRepository notificationRepository;
    private final MemberNotificationRepository memberNotificationRepository;
    private final BookAcquisitionRequestRepository bookAcquisitionRequestRepository;

    public LibrarianInteractionServiceImpl(FeedbackRepository feedbackRepository,
                                           MemberRepository memberRepository,
                                           NotificationRepository notificationRepository,
                                           MemberNotificationRepository memberNotificationRepository,
                                           BookAcquisitionRequestRepository bookAcquisitionRequestRepository) {
        this.feedbackRepository = feedbackRepository;
        this.memberRepository = memberRepository;
        this.notificationRepository = notificationRepository;
        this.memberNotificationRepository = memberNotificationRepository;
        this.bookAcquisitionRequestRepository = bookAcquisitionRequestRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LibrarianReviewResponse> getReviewsForModeration(String status, Pageable pageable) {
        Page<Feedback> feedbacks = feedbackRepository.findAll(pageable);

        return feedbacks.map(fb -> {
            LibrarianReviewResponse res = new LibrarianReviewResponse();
            res.setFeedbackId(fb.getFeedbackId());
            res.setBookTitle(fb.getBook().getTitle());
            res.setMemberName(fb.getMember().getUser().getFullName());
            res.setRating(fb.getRating());
            res.setComment(fb.getComment());
            res.setStatus(fb.getStatus());
            res.setCreatedDate(fb.getCreatedDate());
            res.setLibrarianResponse(fb.getLibrarianResponse());
            res.setResponseDate(fb.getResponseDate());
            return res;
        });
    }

    @Override
    @Transactional
    public void replyReview(Integer feedbackId, LibrarianReviewReplyRequest request) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đánh giá với ID: " + feedbackId));

        if (DELETED_BY_MEMBER_STATUS.equals(feedback.getStatus())) {
            throw new ValidationException("Đánh giá này đã được member xoá nên không thể phản hồi.");
        }

        if (request.getResponse() == null || request.getResponse().trim().isEmpty()) {
            throw new ValidationException("Nội dung phản hồi không được để trống");
        }

        feedback.setLibrarianResponse(request.getResponse().trim());
        feedback.setResponseDate(LocalDateTime.now());

        feedbackRepository.save(feedback);

        sendPersonalNotification(
                feedback.getMember(),
                "Phản hồi đánh giá",
                "Thủ thư đã phản hồi đánh giá của bạn cho sách '" + feedback.getBook().getTitle() + "'."
        );
    }

    @Override
    @Transactional
    public void deleteReview(Integer feedbackId) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đánh giá với ID: " + feedbackId));

        feedbackRepository.delete(feedback);
    }

    private void sendPersonalNotification(Member member, String title, String content) {
        Notification notif = new Notification();
        notif.setTitle(title);
        notif.setContent(content);
        notif.setCreatedDate(LocalDateTime.now());
        notif.setStatus("Active");

        Notification savedNotif = notificationRepository.save(notif);

        MemberNotification mn = new MemberNotification();
        MemberNotificationId id = new MemberNotificationId(
                member.getMemberId(),
                savedNotif.getNotificationId()
        );

        mn.setId(id);
        mn.setMember(member);
        mn.setNotification(savedNotif);
        mn.setIsRead(false);
        mn.setReadDate(null);

        memberNotificationRepository.save(mn);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Member> getAllMembers() {
        return memberRepository.findAll();
    }

    @Override
    @Transactional
    public void sendNotificationToMembers(LibrarianNotificationSendRequest request) {
        if (request.getRecipientType() == null) {
            throw new ValidationException("Vui lòng chọn đối tượng nhận thông báo.");
        }

        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new ValidationException("Tiêu đề không được để trống");
        }

        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new ValidationException("Nội dung không được để trống");
        }

        List<Member> members;

        switch (request.getRecipientType()) {
            case ALL:
                members = memberRepository.findAll();
                break;

            case SELECTED:
                if (request.getMemberIds() == null || request.getMemberIds().isEmpty()) {
                    throw new ValidationException("Vui lòng chọn ít nhất một độc giả.");
                }

                members = memberRepository.findAllById(request.getMemberIds());

                if (members.size() != request.getMemberIds().size()) {
                    throw new ResourceNotFoundException("Có độc giả không tồn tại.");
                }
                break;

            default:
                throw new ValidationException("Loại người nhận không hợp lệ.");
        }

        Notification notification = new Notification();
        notification.setTitle(request.getTitle().trim());
        notification.setContent(request.getContent().trim());
        notification.setStatus("Active");
        notification.setCreatedDate(LocalDateTime.now());

        Notification saved = notificationRepository.save(notification);

        for (Member member : members) {
            MemberNotification mn = new MemberNotification();

            mn.setId(new MemberNotificationId(
                    member.getMemberId(),
                    saved.getNotificationId()
            ));

            mn.setMember(member);
            mn.setNotification(saved);
            mn.setIsRead(false);

            memberNotificationRepository.save(mn);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BookAcquisitionRequest> getBookAcquisitionRequests(Pageable pageable) {
        return bookAcquisitionRequestRepository.findAll(pageable);
    }
}
