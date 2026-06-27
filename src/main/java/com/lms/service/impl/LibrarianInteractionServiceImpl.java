package com.lms.service.impl;

import com.lms.dto.request.LibrarianReviewModerateRequest;
import com.lms.dto.request.MemberReviewSubmitRequest;
import com.lms.dto.response.LibrarianReviewResponse;
import com.lms.entity.*;
import com.lms.exception.ResourceNotFoundException;
import com.lms.repository.*;
import com.lms.service.LibrarianInteractionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.lms.dto.request.LibrarianNotificationSendRequest;
import com.lms.exception.ValidationException;
import java.util.List;

import java.time.LocalDateTime;

@Service
public class LibrarianInteractionServiceImpl implements LibrarianInteractionService {

    private final FeedbackRepository feedbackRepository;
    private final MemberRepository memberRepository;
    private final BookRepository bookRepository;
    private final NotificationRepository notificationRepository;
    private final MemberNotificationRepository memberNotificationRepository;

    public LibrarianInteractionServiceImpl(FeedbackRepository feedbackRepository,
                                           MemberRepository memberRepository,
                                           BookRepository bookRepository,
                                           NotificationRepository notificationRepository,
                                           MemberNotificationRepository memberNotificationRepository) {
        this.feedbackRepository = feedbackRepository;
        this.memberRepository = memberRepository;
        this.bookRepository = bookRepository;
        this.notificationRepository = notificationRepository;
        this.memberNotificationRepository = memberNotificationRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LibrarianReviewResponse> getReviewsForModeration(String status, Pageable pageable) {
        String searchStatus = (status == null || status.isBlank()) ? "PENDING" : status;
        Page<Feedback> feedbacks = feedbackRepository.findByStatus(searchStatus, pageable);

        return feedbacks.map(fb -> {
            LibrarianReviewResponse res = new LibrarianReviewResponse();
            res.setFeedbackId(fb.getFeedbackId());
            res.setBookTitle(fb.getBook().getTitle());
            res.setMemberName(fb.getMember().getUser().getFullName());
            res.setRating(fb.getRating());
            res.setComment(fb.getComment());
            res.setStatus(fb.getStatus());
            res.setCreatedDate(fb.getCreatedDate());
            return res;
        });
    }

    @Override
    @Transactional
    public void moderateReview(Integer feedbackId,
                               LibrarianReviewModerateRequest request) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đánh giá với ID: " + feedbackId));

        String action = request.getAction();
        String bookTitle = feedback.getBook().getTitle();
        String notifMessage = "";

        if ("APPROVE".equalsIgnoreCase(action)) {
            feedback.setStatus("APPROVED");
            notifMessage = "Đánh giá của bạn cho sách '" + bookTitle + "' đã được duyệt.";
        } else if ("REJECT".equalsIgnoreCase(action)) {
            feedback.setStatus("REJECTED");
            notifMessage = "Đánh giá của bạn cho sách '" + bookTitle + "' đã bị từ chối.";
        } else {
            throw new IllegalArgumentException("Hành động không hợp lệ");
        }

        feedbackRepository.save(feedback);

        // Gọi hàm gửi thông báo cho member
        sendPersonalNotification(feedback.getMember(), "Kết quả kiểm duyệt đánh giá", notifMessage);
    }

    @Override
    @Transactional
    public void submitReview(Integer memberId, MemberReviewSubmitRequest request) {
        // 1. Tìm Member (findById trả về Optional, phải get() hoặc orElseThrow())
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Độc giả với ID: " + memberId));

        // 2. Tìm Book (findById trả về Optional)
        Book book = bookRepository.findById(request.getBookId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Sách với ID: " + request.getBookId()));

        // 3. Khởi tạo Feedback và set các thuộc tính
        Feedback feedback = new Feedback();
        feedback.setMember(member);
        feedback.setBook(book);
        feedback.setRating(request.getRating());
        feedback.setComment(request.getComment());
        feedback.setCreatedDate(LocalDateTime.now());
        feedback.setStatus("PENDING"); // Pre-moderation

        // 4. Lưu
        feedbackRepository.save(feedback);
    }

    private void sendPersonalNotification(Member member, String title, String content) {
        // 1. Tạo Notification
        Notification notif = new Notification();
        notif.setTitle(title);
        notif.setContent(content);
        notif.setCreatedDate(LocalDateTime.now());
        notif.setStatus("Active");
        Notification savedNotif = notificationRepository.save(notif);

        // Các bước tạo MemberNotification giữ nguyên
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

                if (request.getMemberIds() == null
                        || request.getMemberIds().isEmpty()) {

                    throw new ValidationException(
                            "Vui lòng chọn ít nhất một độc giả.");
                }

                members = memberRepository.findAllById(request.getMemberIds());

                if (members.size() != request.getMemberIds().size()) {
                    throw new ResourceNotFoundException(
                            "Có độc giả không tồn tại.");
                }

                break;

            default:

                throw new ValidationException(
                        "Loại người nhận không hợp lệ.");
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
                    saved.getNotificationId()));

            mn.setMember(member);
            mn.setNotification(saved);
            mn.setIsRead(false);

            memberNotificationRepository.save(mn);
        }
    }
}