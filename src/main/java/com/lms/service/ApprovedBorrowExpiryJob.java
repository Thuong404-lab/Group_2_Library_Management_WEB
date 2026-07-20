package com.lms.service;

import com.lms.util.BorrowCodeFormatter;

import com.lms.entity.BookItem;
import com.lms.entity.Borrow;
import com.lms.entity.BorrowDetail;
import com.lms.entity.Notification;
import com.lms.entity.MemberNotification;
import com.lms.entity.MemberNotificationId;
import com.lms.enums.NotificationEventType;
import com.lms.enums.NotificationSource;
import com.lms.enums.NotificationType;
import com.lms.repository.BookItemRepository;
import com.lms.repository.BorrowDetailRepository;
import com.lms.repository.BorrowRepository;
import com.lms.repository.NotificationRepository;
import com.lms.repository.MemberNotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class ApprovedBorrowExpiryJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApprovedBorrowExpiryJob.class);

    private final BorrowRepository borrowRepository;
    private final BorrowDetailRepository borrowDetailRepository;
    private final BookItemRepository bookItemRepository;
    private final NotificationRepository notificationRepository;
    private final MemberNotificationRepository memberNotificationRepository;
    private final LocalizedMessageService messages;

    public ApprovedBorrowExpiryJob(BorrowRepository borrowRepository,
                                   BorrowDetailRepository borrowDetailRepository,
                                   BookItemRepository bookItemRepository,
                                   NotificationRepository notificationRepository,
                                   MemberNotificationRepository memberNotificationRepository,
                                   LocalizedMessageService messages) {
        this.borrowRepository = borrowRepository;
        this.borrowDetailRepository = borrowDetailRepository;
        this.bookItemRepository = bookItemRepository;
        this.notificationRepository = notificationRepository;
        this.memberNotificationRepository = memberNotificationRepository;
        this.messages = messages;
    }

    /**
     * Tự động quét hệ thống định kỳ mỗi 10 phút.
     * Tìm các phiếu mượn ở trạng thái Waiting_Pickup có thời gian duyệt vượt quá 48 tiếng để hủy đơn.
     */
    @Scheduled(fixedDelay = 600000)
    @Transactional(rollbackFor = Exception.class)
    public void cancelExpiredApprovedBorrows() {
        LocalDateTime thresholdTime = LocalDateTime.now().minusHours(48);

        // Tối ưu hiệu năng: Chỉ quét lấy ra các phiếu có trạng thái Waiting_Pickup
        List<Borrow> expiredBorrows = borrowRepository.findAllByStatus("Waiting_Pickup").stream()
                .filter(b -> b.getBorrowDate() != null && b.getBorrowDate().isBefore(thresholdTime))
                .toList();

        if (expiredBorrows.isEmpty()) {
            return;
        }

        LOGGER.info("Phát hiện {} phiếu mượn quá hạn 48 giờ chưa nhận sách. Tiến hành hủy tự động...", expiredBorrows.size());

        for (Borrow borrow : expiredBorrows) {
            borrow.setStatus("Canceled");
            borrowRepository.save(borrow);

            List<BorrowDetail> details = borrowDetailRepository.findByBorrowId(borrow.getBorrowId());
            for (BorrowDetail detail : details) {
                detail.setStatus("Canceled");
                borrowDetailRepository.save(detail);

                if (detail.getBookItem() != null) {
                    BookItem item = detail.getBookItem();
                    item.setStatus("Available"); // Giải phóng sách vật lý về trạng thái Sẵn sàng cho mượn
                    bookItemRepository.save(item);
                }
            }

            // Tạo thông báo gửi đến độc giả chỉ rõ điều khoản vi phạm quy định nhận sách và không hoàn phí
            try {
                Notification notif = new Notification();
                messages.prepareNotification(
                        notif,
                        "systemNotification.borrow.pickupExpired.title",
                        "systemNotification.borrow.pickupExpired.content",
                        BorrowCodeFormatter.format(borrow.getBorrowId()));
                notif.setNotificationType(NotificationType.LOAN);
                notif.setEventType(NotificationEventType.LOAN_PICKUP_EXPIRED);
                notif.setNotificationSource(NotificationSource.SYSTEM);
                notif.setCreatedDate(LocalDateTime.now());
                notif.setStatus("Active");
                Notification saved = notificationRepository.save(notif);

                MemberNotification mn = new MemberNotification();
                mn.setId(new MemberNotificationId(borrow.getMember().getMemberId(), saved.getNotificationId()));
                mn.setMember(borrow.getMember());
                mn.setNotification(saved);
                mn.setIsRead(false);
                memberNotificationRepository.save(mn);
            } catch (Exception e) {
                LOGGER.error("Gặp lỗi trong quá trình gửi thông báo hủy phiếu mượn #{}", borrow.getBorrowId(), e);
            }
        }
    }
}
