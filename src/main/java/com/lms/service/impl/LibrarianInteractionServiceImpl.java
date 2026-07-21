package com.lms.service.impl;

import com.lms.dto.request.LibrarianNotificationSendRequest;
import com.lms.dto.request.LibrarianReviewReplyRequest;
import com.lms.dto.response.LibrarianReviewResponse;
import com.lms.entity.*;
import com.lms.exception.ResourceNotFoundException;
import com.lms.exception.ConflictException;
import com.lms.exception.ValidationException;
import com.lms.enums.AcquisitionRequestStatus;
import com.lms.enums.NotificationEventType;
import com.lms.enums.NotificationSource;
import com.lms.enums.NotificationType;
import com.lms.enums.FeedbackStatus;
import com.lms.domain.ReviewPolicy;
import com.lms.repository.*;
import com.lms.service.LibrarianInteractionService;
import com.lms.service.LocalizedMessageService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class LibrarianInteractionServiceImpl implements LibrarianInteractionService {

    private static final MessageSource TEST_FALLBACK_MESSAGES = createFallbackMessages();

    private final FeedbackRepository feedbackRepository;
    private final MemberRepository memberRepository;
    private final NotificationRepository notificationRepository;
    private final MemberNotificationRepository memberNotificationRepository;
    private final BookAcquisitionRequestRepository bookAcquisitionRequestRepository;
    private final StaffAccountRepository staffAccountRepository;

    @Autowired
    private MessageSource messageSource;
    @Autowired
    private LocalizedMessageService localizedMessageService = LocalizedMessageService.fallback();

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
        FeedbackStatus requestedStatus = parseFeedbackStatus(status);
        Page<Feedback> feedbacks = requestedStatus == null
                ? feedbackRepository.findAll(pageable)
                : feedbackRepository.findByStatus(requestedStatus, pageable);

        return feedbacks.map(fb -> {
            LibrarianReviewResponse res = new LibrarianReviewResponse();
            res.setFeedbackId(fb.getFeedbackId());
            res.setBookTitle(fb.getBook().getTitle());
            res.setMemberName(fb.getMember().getUser().getFullName());
            res.setRating(fb.getRating());
            res.setComment(fb.getComment());
            res.setStatus(fb.getStatus().name());
            res.setCreatedDate(fb.getCreatedDate());
            res.setLibrarianResponse(fb.getLibrarianResponse());
            res.setResponseDate(fb.getResponseDate());
            res.setModerationReason(fb.getModerationReason());
            return res;
        });
    }

    @Override
    @Transactional
    public boolean replyReview(Integer feedbackId, LibrarianReviewReplyRequest request) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new ResourceNotFoundException(msg("backend.librarian.review.notFound", feedbackId)));

        if (FeedbackStatus.DELETED_BY_MEMBER == feedback.getStatus()) {
            throw new ConflictException(msg("backend.librarian.review.deletedByMember"));
        }

        if (FeedbackStatus.APPROVED != feedback.getStatus()) {
            throw new ConflictException(msg("backend.librarian.reviewReply.approvedOnly"));
        }

        String normalizedResponse = request.getResponse() == null ? "" : request.getResponse().strip()
                .replaceAll("(?:\\R\\s*){3,}", System.lineSeparator() + System.lineSeparator());

        if (normalizedResponse.isEmpty()) {
            throw new ValidationException(msg("backend.librarian.reviewReply.required"));
        }

        if (normalizedResponse.length() < ReviewPolicy.CONTENT_MIN_LENGTH
                || normalizedResponse.length() > ReviewPolicy.CONTENT_MAX_LENGTH) {
            throw new ValidationException(msg("backend.librarian.reviewReply.range"));
        }

        if (normalizedResponse.codePoints().noneMatch(Character::isLetter)) {
            throw new ValidationException(msg("backend.librarian.reviewReply.letters"));
        }

        boolean isEditing = feedback.getLibrarianResponse() != null
                && !feedback.getLibrarianResponse().isBlank();

        feedback.setLibrarianResponse(normalizedResponse);
        feedback.setResponseDate(LocalDateTime.now());

        feedbackRepository.save(feedback);

        sendPersonalNotification(
                feedback.getMember(),
                NotificationType.REVIEW, NotificationEventType.REVIEW_REPLIED,
                "notification.reviewReply.title",
                "notification.reviewReply.content",
                feedback.getBook().getTitle()
        );

        return isEditing;
    }

    @Override
    @Transactional
    public void approveReview(Integer feedbackId) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new ResourceNotFoundException(msg("backend.librarian.review.notFound", feedbackId)));

        if (FeedbackStatus.DELETED_BY_MEMBER == feedback.getStatus()) {
            throw new ConflictException(msg("backend.librarian.review.deletedByMember"));
        }
        if (FeedbackStatus.APPROVED == feedback.getStatus()) {
            throw new ConflictException(msg("backend.librarian.review.alreadyApproved"));
        }

        feedback.setStatus(FeedbackStatus.APPROVED);
        feedback.setModerationReason(null);
        feedback.setModeratedDate(LocalDateTime.now());
        feedbackRepository.save(feedback);

        sendPersonalNotification(feedback.getMember(), NotificationType.REVIEW,
                NotificationEventType.REVIEW_APPROVED,
                "notification.reviewApproved.title", "notification.reviewApproved.content",
                feedback.getBook().getTitle());
    }

    @Override
    @Transactional
    public void rejectReview(Integer feedbackId, String reason) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new ResourceNotFoundException(msg("backend.librarian.review.notFound", feedbackId)));

        if (FeedbackStatus.DELETED_BY_MEMBER == feedback.getStatus()) {
            throw new ConflictException(msg("backend.librarian.review.deletedByMember"));
        }
        if (FeedbackStatus.REJECTED == feedback.getStatus()) {
            throw new ConflictException(msg("backend.librarian.review.alreadyRejected"));
        }

        String normalizedReason = normalizeSingleLine(reason);
        if (normalizedReason.length() < ReviewPolicy.CONTENT_MIN_LENGTH
                || normalizedReason.length() > ReviewPolicy.MODERATION_REASON_MAX_LENGTH) {
            throw new ValidationException(msg("backend.librarian.reviewReject.reasonRange"));
        }
        if (normalizedReason.codePoints().noneMatch(Character::isLetter)) {
            throw new ValidationException(msg("backend.librarian.reviewReject.reasonLetters"));
        }

        feedback.setStatus(FeedbackStatus.REJECTED);
        feedback.setModerationReason(normalizedReason);
        feedback.setModeratedDate(LocalDateTime.now());
        feedback.setLibrarianResponse(null);
        feedback.setResponseDate(null);
        feedbackRepository.save(feedback);

        sendPersonalNotification(feedback.getMember(), NotificationType.REVIEW,
                NotificationEventType.REVIEW_REJECTED,
                "notification.reviewRejected.title", "notification.reviewRejected.content",
                feedback.getBook().getTitle(), normalizedReason);
    }

    private FeedbackStatus parseFeedbackStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return FeedbackStatus.valueOf(status.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ValidationException(msg("backend.librarian.review.statusInvalid"));
        }
    }

    private String normalizeSingleLine(String value) {
        return value == null ? "" : value.strip().replaceAll("\\s+", " ");
    }

    private void sendPersonalNotification(Member member,
                                          NotificationType type,
                                          NotificationEventType eventType,
                                          String titleKey,
                                          String contentKey,
                                          Object... arguments) {
        Notification notif = new Notification();
        localizedMessageService.prepareNotification(notif, titleKey, contentKey, arguments);
        notif.setNotificationType(type);
        notif.setEventType(eventType);
        notif.setNotificationSource(NotificationSource.LIBRARIAN);
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
        return memberRepository.findAllWithActiveAccount();
    }

    @Override
    @Transactional
    public void sendNotificationToMembers(LibrarianNotificationSendRequest request, String senderUsername) {
        if (request.getRecipientType() == null) {
            throw new ValidationException(msg("backend.librarian.notification.recipientRequired"));
        }

        if (request.getNotificationType() == null) {
            throw new ValidationException(msg("backend.librarian.notification.typeRequired"));
        }

        if (!request.getNotificationType().isManualSelectable()) {
            throw new ValidationException(msg("backend.librarian.notification.typeInvalidForManual"));
        }

        String normalizedTitle = request.getTitle() == null ? "" : request.getTitle().trim().replaceAll("\\s+", " ");
        String normalizedContent = request.getContent() == null ? "" : request.getContent().strip()
                .replaceAll("(?:\\R\\s*){3,}", System.lineSeparator() + System.lineSeparator());

        if (normalizedTitle.isEmpty()) {
            throw new ValidationException(msg("backend.librarian.notification.titleRequired"));
        }

        if (normalizedTitle.length() < 5 || normalizedTitle.length() > 150) {
            throw new ValidationException(msg("backend.librarian.notification.titleRange"));
        }

        if (normalizedContent.isEmpty()) {
            throw new ValidationException(msg("backend.librarian.notification.contentRequired"));
        }

        if (normalizedContent.length() < 10 || normalizedContent.length() > 2000) {
            throw new ValidationException(msg("backend.librarian.notification.contentRange"));
        }

        if (normalizedContent.equalsIgnoreCase(normalizedTitle)) {
            throw new ValidationException(msg("backend.librarian.notification.contentDifferent"));
        }

        Staff sender = staffAccountRepository.findByUsername(senderUsername)
                .map(StaffAccount::getStaff)
                .orElseThrow(() -> new ResourceNotFoundException(
                        msg("backend.librarian.notification.senderNotFound")));

        List<Member> members;

        switch (request.getRecipientType()) {
            case ALL:
                members = memberRepository.findAllWithActiveAccount();
                break;

            case SELECTED:
                if (request.getMemberIds() == null || request.getMemberIds().isEmpty()) {
                    throw new ValidationException(msg("backend.librarian.notification.memberRequired"));
                }

                members = memberRepository.findAllWithActiveAccountByMemberIdIn(request.getMemberIds());

                if (members.size() != request.getMemberIds().size()) {
                    throw new ResourceNotFoundException(msg("backend.librarian.notification.memberUnavailable"));
                }
                break;

            default:
                throw new ValidationException(msg("backend.librarian.notification.recipientInvalid"));
        }

        if (members.isEmpty()) {
            throw new ValidationException(msg("backend.librarian.notification.noActiveMembers"));
        }

        Notification notification = new Notification();
        notification.setTitle(normalizedTitle);
        notification.setContent(normalizedContent);
        notification.setNotificationType(request.getNotificationType());
        notification.setEventType(NotificationEventType.MANUAL);
        notification.setNotificationSource(NotificationSource.LIBRARIAN);
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
                NotificationType.ACQUISITION, NotificationEventType.ACQUISITION_APPROVED,
                "notification.acquisitionApproved.title",
                "notification.acquisitionApproved.content",
                request.getTitle()
        );
    }

    @Override
    @Transactional
    public void rejectBookAcquisitionRequest(Integer requestId, String reason) {
        String normalizedReason = reason == null ? "" : reason.strip()
                .replaceAll("(?:\\R\\s*){3,}", System.lineSeparator() + System.lineSeparator());
        if (normalizedReason.isEmpty()) {
            throw new ValidationException(msg("librarian.acquisition.validationRequired"));
        }
        if (normalizedReason.length() < 5) {
            throw new ValidationException(msg("librarian.acquisition.validationMinimum"));
        }
        if (normalizedReason.length() > 500) {
            throw new ValidationException(msg("librarian.acquisition.validationMaximum"));
        }
        if (normalizedReason.codePoints().noneMatch(Character::isLetter)) {
            throw new ValidationException(msg("librarian.acquisition.validationLetters"));
        }

        BookAcquisitionRequest request = getPendingAcquisitionRequest(requestId);
        request.setStatus(AcquisitionRequestStatus.REJECTED);
        request.setDecisionNote(normalizedReason);
        request.setProcessedDate(LocalDateTime.now());
        bookAcquisitionRequestRepository.save(request);
        sendPersonalNotification(
                request.getMember(),
                NotificationType.ACQUISITION, NotificationEventType.ACQUISITION_REJECTED,
                "notification.acquisitionRejected.title",
                "notification.acquisitionRejected.content",
                request.getTitle(), normalizedReason
        );
    }

    private BookAcquisitionRequest getPendingAcquisitionRequest(Integer requestId) {
        BookAcquisitionRequest request = bookAcquisitionRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException(msg("backend.librarian.acquisition.notFound")));
        if (request.getStatus() != AcquisitionRequestStatus.PENDING) {
            throw new ConflictException(msg("backend.librarian.acquisition.alreadyProcessed"));
        }
        return request;
    }

    private String msg(String key, Object... arguments) {
        MessageSource source = messageSource == null ? TEST_FALLBACK_MESSAGES : messageSource;
        Locale locale = messageSource == null ? Locale.forLanguageTag("vi") : LocaleContextHolder.getLocale();
        return source.getMessage(key, arguments, locale);
    }

    private static MessageSource createFallbackMessages() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("messages");
        source.setDefaultEncoding("UTF-8");
        return source;
    }
}
