package com.lms.service.impl;

import com.lms.dto.request.LibrarianNotificationSendRequest;
import com.lms.dto.request.LibrarianReviewReplyRequest;
import com.lms.dto.response.LibrarianReviewResponse;
import com.lms.entity.*;
import com.lms.exception.ResourceNotFoundException;
import com.lms.exception.ConflictException;
import com.lms.exception.ValidationException;
import com.lms.enums.AcquisitionRequestStatus;
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
    private final StaffAccountRepository staffAccountRepository;

    public LibrarianInteractionServiceImpl(FeedbackRepository feedbackRepository,
                                           MemberRepository memberRepository,
                                           NotificationRepository notificationRepository,
                                           MemberNotificationRepository memberNotificationRepository,
                                           BookAcquisitionRequestRepository bookAcquisitionRequestRepository,
                                           StaffAccountRepository staffAccountRepository) {
        this.feedbackRepository = feedbackRepository;
        this.memberRepository = memberRepository;
        this.notificationRepository = notificationRepository;
        this.memberNotificationRepository = memberNotificationRepository;
        this.bookAcquisitionRequestRepository = bookAcquisitionRequestRepository;
        this.staffAccountRepository = staffAccountRepository;
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
    public boolean replyReview(Integer feedbackId, LibrarianReviewReplyRequest request) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đánh giá với ID: " + feedbackId));

        if (DELETED_BY_MEMBER_STATUS.equals(feedback.getStatus())) {
            throw new ConflictException("Đánh giá này đã được member xoá nên không thể phản hồi.");
        }

        String normalizedResponse = request.getResponse() == null ? "" : request.getResponse().strip()
                .replaceAll("(?:\\R\\s*){3,}", System.lineSeparator() + System.lineSeparator());

        if (normalizedResponse.isEmpty()) {
            throw new ValidationException("Nội dung phản hồi không được để trống");
        }

        if (normalizedResponse.length() < 5 || normalizedResponse.length() > 1000) {
            throw new ValidationException("Nội dung phản hồi phải có từ 5 đến 1000 ký tự.");
        }

        if (normalizedResponse.codePoints().noneMatch(Character::isLetter)) {
            throw new ValidationException("Nội dung phản hồi không được chỉ gồm số hoặc ký tự đặc biệt.");
        }

        boolean isEditing = feedback.getLibrarianResponse() != null
                && !feedback.getLibrarianResponse().isBlank();

        feedback.setLibrarianResponse(normalizedResponse);
        feedback.setResponseDate(LocalDateTime.now());

        feedbackRepository.save(feedback);

        sendPersonalNotification(
                feedback.getMember(),
                "Phản hồi đánh giá",
                "Thủ thư đã phản hồi đánh giá của bạn cho sách '" + feedback.getBook().getTitle() + "'."
        );

        return isEditing;
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
    public void sendNotificationToMembers(LibrarianNotificationSendRequest request, String senderUsername) {
        if (request.getRecipientType() == null) {
            throw new ValidationException("Vui lòng chọn đối tượng nhận thông báo.");
        }

        if (request.getNotificationType() == null) {
            throw new ValidationException("Vui lòng chọn loại thông báo.");
        }

        String normalizedTitle = request.getTitle() == null ? "" : request.getTitle().trim().replaceAll("\\s+", " ");
        String normalizedContent = request.getContent() == null ? "" : request.getContent().strip()
                .replaceAll("(?:\\R\\s*){3,}", System.lineSeparator() + System.lineSeparator());

        if (normalizedTitle.isEmpty()) {
            throw new ValidationException("Tiêu đề không được để trống");
        }

        if (normalizedTitle.length() < 5 || normalizedTitle.length() > 150) {
            throw new ValidationException("Tiêu đề phải có từ 5 đến 150 ký tự.");
        }

        if (normalizedContent.isEmpty()) {
            throw new ValidationException("Nội dung không được để trống");
        }

        if (normalizedContent.length() < 10 || normalizedContent.length() > 2000) {
            throw new ValidationException("Nội dung phải có từ 10 đến 2000 ký tự.");
        }

        if (normalizedContent.equalsIgnoreCase(normalizedTitle)) {
            throw new ValidationException("Nội dung không được giống hoàn toàn tiêu đề.");
        }

        Staff sender = staffAccountRepository.findByUsername(senderUsername)
                .map(StaffAccount::getStaff)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy thủ thư gửi thông báo."));

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
        notification.setTitle(normalizedTitle);
        notification.setContent(normalizedContent);
        notification.setNotificationType(request.getNotificationType());
        notification.setStaff(sender);
        notification.setStatus("Active");
        notification.setCreatedDate(LocalDateTime.now());

        Notification saved = notificationRepository.save(notification);

        List<MemberNotification> memberNotifications = members.stream().map(member -> {
            MemberNotification mn = new MemberNotification();

            mn.setId(new MemberNotificationId(
                    member.getMemberId(),
                    saved.getNotificationId()
            ));

            mn.setMember(member);
            mn.setNotification(saved);
            mn.setIsRead(false);
            mn.setReadDate(null);

            return mn;
        }).toList();

        memberNotificationRepository.saveAll(memberNotifications);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BookAcquisitionRequest> getBookAcquisitionRequests(Pageable pageable) {
        return bookAcquisitionRequestRepository.findAll(pageable);
    }

    @Override
    @Transactional
    public void approveBookAcquisitionRequest(Integer requestId) {
        BookAcquisitionRequest request = getPendingAcquisitionRequest(requestId);
        request.setStatus(AcquisitionRequestStatus.APPROVED);
        request.setDecisionNote(null);
        request.setProcessedDate(LocalDateTime.now());
        bookAcquisitionRequestRepository.save(request);
        sendPersonalNotification(
                request.getMember(),
                "Đề xuất sách đã được duyệt",
                "Đề xuất bổ sung sách '" + request.getTitle() + "' của bạn đã được thư viện chấp nhận."
        );
    }

    @Override
    @Transactional
    public void rejectBookAcquisitionRequest(Integer requestId, String reason) {
        String normalizedReason = reason == null ? "" : reason.strip()
                .replaceAll("(?:\\R\\s*){3,}", System.lineSeparator() + System.lineSeparator());
        if (normalizedReason.isEmpty()) {
            throw new ValidationException("Lý do từ chối không được để trống.");
        }
        if (normalizedReason.length() < 5) {
            throw new ValidationException("Lý do từ chối phải có ít nhất 5 ký tự.");
        }
        if (normalizedReason.length() > 500) {
            throw new ValidationException("Lý do từ chối không được vượt quá 500 ký tự.");
        }
        if (normalizedReason.codePoints().noneMatch(Character::isLetter)) {
            throw new ValidationException("Lý do từ chối không được chỉ gồm số hoặc ký tự đặc biệt.");
        }

        BookAcquisitionRequest request = getPendingAcquisitionRequest(requestId);
        request.setStatus(AcquisitionRequestStatus.REJECTED);
        request.setDecisionNote(normalizedReason);
        request.setProcessedDate(LocalDateTime.now());
        bookAcquisitionRequestRepository.save(request);
        sendPersonalNotification(
                request.getMember(),
                "Đề xuất sách chưa được chấp nhận",
                "Đề xuất bổ sung sách '" + request.getTitle() + "' của bạn đã bị từ chối. Lý do: " + reason.trim()
        );
    }

    private BookAcquisitionRequest getPendingAcquisitionRequest(Integer requestId) {
        BookAcquisitionRequest request = bookAcquisitionRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đề xuất sách."));
        if (request.getStatus() != AcquisitionRequestStatus.PENDING) {
            throw new ConflictException("Đề xuất này đã được xử lý trước đó.");
        }
        return request;
    }
}
