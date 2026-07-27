package com.lms.service.impl;

import com.lms.dto.request.LibrarianNotificationSendRequest;
import com.lms.dto.request.LibrarianReviewReplyRequest;
import com.lms.dto.response.LibrarianReviewResponse;
import com.lms.dto.response.LibrarianNotificationHistoryResponse;
import com.lms.dto.response.NotificationRecipientSearchResponse;
import com.lms.dto.response.NotificationSendResult;
import com.lms.entity.*;
import com.lms.exception.ResourceNotFoundException;
import com.lms.exception.ConflictException;
import com.lms.exception.ValidationException;
import com.lms.enums.BookRequestStatus;
import com.lms.enums.NotificationEventType;
import com.lms.enums.NotificationSource;
import com.lms.enums.NotificationType;
import com.lms.enums.FeedbackStatus;
import com.lms.util.ReviewPolicy;
import com.lms.util.BookRequestPolicy;
import com.lms.repository.*;
import com.lms.service.LibrarianInteractionService;
import com.lms.service.LocalizedMessageService;
import com.lms.service.NotificationComposePolicy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.OptimisticLockingFailureException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class LibrarianInteractionServiceImpl implements LibrarianInteractionService {

    private static final String MEMBER_CODE_PREFIX = "MEM-";
    private static final MessageSource TEST_FALLBACK_MESSAGES = createFallbackMessages();

    private final FeedbackRepository feedbackRepository;
    private final MemberRepository memberRepository;
    private final NotificationRepository notificationRepository;
    private final MemberNotificationRepository memberNotificationRepository;
    private final BookRequestRepository bookRequestRepository;
    private final StaffAccountRepository staffAccountRepository;

    @Autowired
    private MessageSource messageSource;
    @Autowired
    private LocalizedMessageService localizedMessageService = LocalizedMessageService.fallback();

    public LibrarianInteractionServiceImpl(FeedbackRepository feedbackRepository,
                                           MemberRepository memberRepository,
                                           NotificationRepository notificationRepository,
                                           MemberNotificationRepository memberNotificationRepository,
                                           BookRequestRepository bookRequestRepository,
                                           StaffAccountRepository staffAccountRepository) {
        this.feedbackRepository = feedbackRepository;
        this.memberRepository = memberRepository;
        this.notificationRepository = notificationRepository;
        this.memberNotificationRepository = memberNotificationRepository;
        this.bookRequestRepository = bookRequestRepository;
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

    @Override
    @Transactional
    public void deleteReview(Integer feedbackId) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        msg("backend.librarian.review.notFound", feedbackId)));
        feedbackRepository.delete(feedback);
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
        notif.setStatus(Notification.STATUS_ACTIVE);

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
    public long countActiveMembers() {
        return memberRepository.countActiveAccounts();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationRecipientSearchResponse> searchNotificationRecipients(String query, Pageable pageable) {
        String normalized = query == null ? "" : query.strip();
        if (normalized.length() < NotificationComposePolicy.MEMBER_SEARCH_MIN_LENGTH) {
            return Page.empty(pageable);
        }
        Integer memberId = parseMemberId(normalized);
        return memberRepository.searchActiveNotificationRecipients(normalized, memberId, pageable)
                .map(this::toRecipientResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationRecipientSearchResponse> getNotificationRecipients(List<Integer> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) return List.of();
        return memberRepository.findAllWithActiveAccountByMemberIdIn(memberIds).stream()
                .map(this::toRecipientResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LibrarianNotificationHistoryResponse> getRecentManualNotifications(
            NotificationType notificationType, Pageable pageable) {
        Page<Notification> notifications = notificationType == null
                ? notificationRepository.findByEventTypeOrderByCreatedDateDesc(
                        NotificationEventType.MANUAL, pageable)
                : notificationRepository.findByEventTypeAndNotificationTypeOrderByCreatedDateDesc(
                        NotificationEventType.MANUAL, notificationType, pageable);
        return notifications
                .map(notification -> new LibrarianNotificationHistoryResponse(
                        notification.getNotificationId(), notification.getTitle(), notification.getNotificationType(),
                        notification.getStaff() != null && notification.getStaff().getUser() != null
                                ? notification.getStaff().getUser().getFullName() : "",
                        notification.getCreatedDate(),
                        memberNotificationRepository.countByNotification_NotificationId(notification.getNotificationId()),
                        memberNotificationRepository.countByNotification_NotificationIdAndIsReadTrue(notification.getNotificationId())
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Member> getAllMembers() {
        return memberRepository.findAllWithActiveAccount();
    }

    @Override
    @Transactional
    public NotificationSendResult sendNotificationToMembers(LibrarianNotificationSendRequest request, String senderUsername) {
        Map<String, String> validationErrors = NotificationComposePolicy.normalizeAndValidate(request);
        if (!validationErrors.isEmpty()) {
            throw new ValidationException(msg(validationErrors.values().iterator().next()));
        }

        Staff sender = staffAccountRepository.findByUsernameForNotificationSend(senderUsername)
                .map(StaffAccount::getStaff)
                .orElseThrow(() -> new ResourceNotFoundException(
                        msg("backend.librarian.notification.senderNotFound")));

        Notification previous = notificationRepository.findByRequestKey(request.getRequestToken()).orElse(null);
        if (previous != null) {
            if (previous.getStaff() == null || !previous.getStaff().getStaffId().equals(sender.getStaffId())) {
                throw new ValidationException(msg("backend.librarian.notification.requestExpired"));
            }
            return new NotificationSendResult(previous.getNotificationId(),
                    Math.toIntExact(memberNotificationRepository.countByNotification_NotificationId(previous.getNotificationId())),
                    previous.getCreatedDate(), true);
        }

        List<Member> members;

        switch (request.getRecipientType()) {
            case ALL:
                members = memberRepository.findAllWithActiveAccount();
                break;

            case SELECTED:
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
        notification.setTitle(request.getTitle());
        notification.setContent(request.getContent());
        notification.setNotificationType(request.getNotificationType());
        notification.setEventType(NotificationEventType.MANUAL);
        notification.setNotificationSource(NotificationSource.LIBRARIAN);
        notification.setStaff(sender);
        notification.setStatus(Notification.STATUS_ACTIVE);
        notification.setCreatedDate(LocalDateTime.now());
        notification.setRequestKey(request.getRequestToken());

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
        return new NotificationSendResult(saved.getNotificationId(), memberNotifications.size(),
                saved.getCreatedDate(), false);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BookRequest> getBookRequests(Pageable pageable) {
        return getBookRequests(null, null, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BookRequest> getBookRequests(String status, String keyword, Pageable pageable) {
        BookRequestStatus requestedStatus = parseBookRequestStatus(status);
        String normalizedKeyword = keyword == null ? "" : keyword.strip().replaceAll("\\s+", " ");
        if (normalizedKeyword.length() > BookRequestPolicy.SEARCH_KEYWORD_MAX_LENGTH) {
            throw new ValidationException(msg("backend.librarian.bookRequest.keywordMaximum"));
        }
        return bookRequestRepository.searchForModeration(
                requestedStatus, normalizedKeyword, pageable);
    }

    @Override
    @Transactional
    public void approveBookRequest(Integer requestId, String note, String staffUsername) {
        BookRequest request = getPendingBookRequest(requestId);
        String normalizedNote = validateDecisionNote(note);
        request.setStatus(BookRequestStatus.APPROVED);
        request.setDecisionNote(normalizedNote);
        request.setProcessedDate(LocalDateTime.now());
        request.setProcessedBy(getStaff(staffUsername));
        saveBookRequestDecision(request);
        sendPersonalNotification(
                request.getMember(),
                NotificationType.ACQUISITION, NotificationEventType.ACQUISITION_APPROVED,
                "notification.bookRequestApproved.title",
                "notification.bookRequestApproved.content",
                request.getTitle(), normalizedNote
        );
    }

    @Override
    @Transactional
    public void rejectBookRequest(Integer requestId, String reason, String staffUsername) {
        String normalizedReason = validateDecisionNote(reason);
        BookRequest request = getPendingBookRequest(requestId);
        request.setStatus(BookRequestStatus.REJECTED);
        request.setDecisionNote(normalizedReason);
        request.setProcessedDate(LocalDateTime.now());
        request.setProcessedBy(getStaff(staffUsername));
        saveBookRequestDecision(request);
        sendPersonalNotification(
                request.getMember(),
                NotificationType.ACQUISITION, NotificationEventType.ACQUISITION_REJECTED,
                "notification.bookRequestRejected.title",
                "notification.bookRequestRejected.content",
                request.getTitle(), normalizedReason
        );
    }

    private BookRequest getPendingBookRequest(Integer requestId) {
        BookRequest request = bookRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException(msg("backend.librarian.bookRequest.notFound")));
        if (request.getStatus() != BookRequestStatus.PENDING) {
            throw new ConflictException(msg("backend.librarian.bookRequest.alreadyProcessed"));
        }
        return request;
    }

    private BookRequestStatus parseBookRequestStatus(String status) {
        if (status == null || status.isBlank()) return null;
        try {
            return BookRequestStatus.valueOf(status.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ValidationException(msg("backend.librarian.bookRequest.statusInvalid"));
        }
    }

    private String validateDecisionNote(String value) {
        String normalized = value == null ? "" : value.strip()
                .replaceAll("(?:\\R\\s*){3,}", System.lineSeparator() + System.lineSeparator());
        if (normalized.isEmpty()) {
            throw new ValidationException(msg("librarian.bookRequest.validationRequired"));
        }
        if (normalized.length() < BookRequestPolicy.DECISION_NOTE_MIN_LENGTH) {
            throw new ValidationException(msg("librarian.bookRequest.validationMinimum"));
        }
        if (normalized.length() > BookRequestPolicy.DECISION_NOTE_MAX_LENGTH) {
            throw new ValidationException(msg("librarian.bookRequest.validationMaximum"));
        }
        if (normalized.codePoints().noneMatch(Character::isLetter)) {
            throw new ValidationException(msg("librarian.bookRequest.validationLetters"));
        }
        return normalized;
    }

    private Staff getStaff(String username) {
        return staffAccountRepository.findByUsername(username)
                .map(StaffAccount::getStaff)
                .orElseThrow(() -> new ResourceNotFoundException(
                        msg("backend.librarian.bookRequest.staffNotFound")));
    }

    private void saveBookRequestDecision(BookRequest request) {
        try {
            bookRequestRepository.saveAndFlush(request);
        } catch (OptimisticLockingFailureException exception) {
            throw new ConflictException(msg("backend.bookRequest.dataChanged"), exception);
        }
    }

    private NotificationRecipientSearchResponse toRecipientResponse(Member member) {
        User user = member.getUser();
        return new NotificationRecipientSearchResponse(
                member.getMemberId(),
                MEMBER_CODE_PREFIX + member.getMemberId(),
                user != null ? user.getFullName() : "",
                user != null ? user.getEmail() : "",
                user != null ? user.getPhone() : ""
        );
    }

    private Integer parseMemberId(String query) {
        String candidate = query.toUpperCase(Locale.ROOT).startsWith(MEMBER_CODE_PREFIX)
                ? query.substring(MEMBER_CODE_PREFIX.length()).strip() : query;
        try {
            return Integer.valueOf(candidate);
        } catch (NumberFormatException ignored) {
            return null;
        }
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
