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

import java.time.LocalDateTime;

@Service
public class LibrarianInteractionServiceImpl implements LibrarianInteractionService {

    private final FeedbackRepository feedbackRepository;
    private final MemberRepository memberRepository;
    private final BookRepository bookRepository;
    private final NotificationRepository notificationRepository;
    private final MemberNotificationRepository memberNotificationRepository;
    private final StaffRepository staffRepository;

    public LibrarianInteractionServiceImpl(FeedbackRepository feedbackRepository,
                                           MemberRepository memberRepository,
                                           BookRepository bookRepository,
                                           NotificationRepository notificationRepository,
                                           MemberNotificationRepository memberNotificationRepository,
                                           StaffRepository staffRepository) {
        this.feedbackRepository = feedbackRepository;
        this.memberRepository = memberRepository;
        this.bookRepository = bookRepository;
        this.notificationRepository = notificationRepository;
        this.memberNotificationRepository = memberNotificationRepository;
        this.staffRepository = staffRepository;
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
    public void moderateReview(Integer feedbackId, LibrarianReviewModerateRequest request, Integer staffId) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đánh giá với ID: " + feedbackId));

        String action = request.getAction();
        String bookTitle = feedback.getBook().getTitle();
        String notifMessage = "";

        if ("APPROVE".equalsIgnoreCase(action)) {
            feedback.setStatus("APPROVED");
            notifMessage = "Đánh giá của bạn cho sách '" + bookTitle + "' đã được duyệt và đang hiển thị công khai.";
        } else if ("REJECT".equalsIgnoreCase(action)) {
            feedback.setStatus("REJECTED");
            notifMessage = "Đánh giá của bạn cho sách '" + bookTitle + "' đã bị từ chối do vi phạm tiêu chuẩn.";
        } else {
            throw new IllegalArgumentException("Hành động không hợp lệ");
        }

        feedbackRepository.save(feedback);

        sendPersonalNotification(feedback.getMember(), staffId, "Kết quả kiểm duyệt đánh giá", notifMessage);
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

    private void sendPersonalNotification(Member member, Integer staffId, String title, String content) {
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Thủ thư"));

        // 1. Tạo Notification
        Notification notif = new Notification();
        notif.setStaff(staff);
        notif.setTitle(title);
        notif.setContent(content);
        notif.setCreatedDate(LocalDateTime.now());
        notif.setStatus("Active");
        Notification savedNotif = notificationRepository.save(notif);


        // BƯỚC 1: Tạo đối tượng MemberNotification
        MemberNotification mn = new MemberNotification();

        // BƯỚC 2: Khởi tạo khóa chính phức hợp
        MemberNotificationId id = new MemberNotificationId(
                member.getMemberId(),
                savedNotif.getNotificationId()
        );

        // BƯỚC 3: Gán ID và các thực thể liên quan
        mn.setId(id);
        mn.setMember(member);
        mn.setNotification(savedNotif);
        mn.setIsRead(false);
        mn.setReadDate(null);

        // BƯỚC 4: Save
        memberNotificationRepository.save(mn);
    }
}