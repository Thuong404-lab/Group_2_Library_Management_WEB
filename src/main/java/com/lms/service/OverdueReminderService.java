package com.lms.service;

import com.lms.entity.BorrowDetail;
import com.lms.entity.Member;
import com.lms.entity.MemberNotification;
import com.lms.entity.MemberNotificationId;
import com.lms.entity.Notification;
import com.lms.exception.ConflictException;
import com.lms.exception.ResourceNotFoundException;
import com.lms.exception.ValidationException;
import com.lms.repository.BorrowDetailRepository;
import com.lms.repository.MemberNotificationRepository;
import com.lms.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Set;

/**
 * Sends read-only overdue reminders without creating a fine transaction.
 * Maintained by Pham Kien Quoc for the fine-management flow.
 */
@Service
public class OverdueReminderService {
    @Autowired
    private LocalizedMessageService localizedMessageService = LocalizedMessageService.fallback();
    private static final Set<String> REMINDABLE_STATUSES =
            Set.of("BORROWED", "OVERDUE", "RETURN_PENDING");
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final BorrowDetailRepository borrowDetailRepository;
    private final NotificationRepository notificationRepository;
    private final MemberNotificationRepository memberNotificationRepository;

    public OverdueReminderService(
            BorrowDetailRepository borrowDetailRepository,
            NotificationRepository notificationRepository,
            MemberNotificationRepository memberNotificationRepository) {
        this.borrowDetailRepository = borrowDetailRepository;
        this.notificationRepository = notificationRepository;
        this.memberNotificationRepository = memberNotificationRepository;
    }

    @Transactional
    public void sendReturnReminder(Integer borrowDetailId) {
        BorrowDetail detail = borrowDetailRepository.findById(borrowDetailId)
                .orElseThrow(() -> new ResourceNotFoundException(localizedMessageService.get("backend.overdue.loanNotFound")));

        validateRemindable(detail);

        Member member = detail.getBorrow().getMember();
        long overdueDays = Math.max(
                ChronoUnit.DAYS.between(detail.getDueDate().toLocalDate(), LocalDate.now()),
                1L);
        String reminderMarker = localizedMessageService.get("systemNotification.overdue.marker", detail.getBorrowDetailId());

        boolean remindedRecently = memberNotificationRepository
                .findByMemberMemberIdAndNotificationContentContainingIgnoreCaseOrderByNotificationCreatedDateDesc(
                        member.getMemberId(),
                        reminderMarker)
                .stream()
                .map(MemberNotification::getNotification)
                .filter(notification -> notification != null && notification.getCreatedDate() != null)
                .anyMatch(notification -> notification.getCreatedDate().isAfter(LocalDateTime.now().minusHours(24)));

        if (remindedRecently) {
            throw new ConflictException(localizedMessageService.get("backend.overdue.alreadyReminded"));
        }

        String bookTitle = detail.getBook() == null || detail.getBook().getTitle() == null
                ? localizedMessageService.get("systemNotification.overdue.unknownBook")
                : detail.getBook().getTitle();

        Notification notification = new Notification();
        notification.setTitle(localizedMessageService.get("systemNotification.overdue.title"));
        notification.setContent(localizedMessageService.get("systemNotification.overdue.content", bookTitle, overdueDays,
                detail.getDueDate().format(DATE_FORMATTER), reminderMarker));
        notification.setCreatedDate(LocalDateTime.now());
        notification.setStatus("Active");
        notification = notificationRepository.save(notification);

        MemberNotification memberNotification = new MemberNotification();
        memberNotification.setId(new MemberNotificationId(
                member.getMemberId(),
                notification.getNotificationId()));
        memberNotification.setMember(member);
        memberNotification.setNotification(notification);
        memberNotification.setIsRead(false);
        memberNotificationRepository.save(memberNotification);
    }

    private void validateRemindable(BorrowDetail detail) {
        if (detail.getBorrow() == null
                || detail.getBorrow().getMember() == null
                || detail.getBorrow().getMember().getMemberId() == null) {
            throw new ValidationException(localizedMessageService.get("backend.overdue.invalidMember"));
        }
        if (detail.getDueDate() == null
                || !detail.getDueDate().toLocalDate().isBefore(LocalDate.now())
                || detail.getReturnDate() != null) {
            throw new ConflictException(localizedMessageService.get("backend.overdue.noLongerOverdue"));
        }

        String status = detail.getStatus() == null
                ? ""
                : detail.getStatus().trim().toUpperCase(Locale.ROOT);
        if (!REMINDABLE_STATUSES.contains(status)) {
            throw new ConflictException(localizedMessageService.get("backend.overdue.statusNotRemindable"));
        }
    }
}
