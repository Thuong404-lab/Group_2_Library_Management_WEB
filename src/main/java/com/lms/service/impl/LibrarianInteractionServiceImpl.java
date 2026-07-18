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

    private static final String DELETED_BY_MEMBER_STATUS = "DELETED_BY_MEMBER";
    private static final MessageSource TEST_FALLBACK_MESSAGES = createFallbackMessages();

    private final FeedbackRepository feedbackRepository;
    private final MemberRepository memberRepository;
    private final NotificationRepository notificationRepository;
    private final MemberNotificationRepository memberNotificationRepository;
    private final BookAcquisitionRequestRepository bookAcquisitionRequestRepository;
    private final StaffAccountRepository staffAccountRepository;

    @Autowired
    private MessageSource messageSource;

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
                .orElseThrow(() -> new ResourceNotFoundException(msg("backend.librarian.review.notFound", feedbackId)));

        if (DELETED_BY_MEMBER_STATUS.equals(feedback.getStatus())) {
            throw new ConflictException(msg("backend.librarian.review.deletedByMember"));
        }

        String normalizedResponse = request.getResponse() == null ? "" : request.getResponse().strip()
                .replaceAll("(?:\\R\\s*){3,}", System.lineSeparator() + System.lineSeparator());

        if (normalizedResponse.isEmpty()) {
            throw new ValidationException(msg("backend.librarian.reviewReply.required"));
        }

        if (normalizedResponse.length() < 5 || normalizedResponse.length() > 1000) {
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
                msg("notification.reviewReply.title"),
                msg("notification.reviewReply.content", feedback.getBook().getTitle())
        );

        return isEditing;
    }

    @Override
    @Transactional
    public void deleteReview(Integer feedbackId) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new ResourceNotFoundException(msg("backend.librarian.review.notFound", feedbackId)));

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
            throw new ValidationException(msg("backend.librarian.notification.recipientRequired"));
        }

        if (request.getNotificationType() == null) {
            throw new ValidationException(msg("backend.librarian.notification.typeRequired"));
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
                members = memberRepository.findAll();
                break;

            case SELECTED:
                if (request.getMemberIds() == null || request.getMemberIds().isEmpty()) {
                    throw new ValidationException(msg("backend.librarian.notification.memberRequired"));
                }

                members = memberRepository.findAllById(request.getMemberIds());

                if (members.size() != request.getMemberIds().size()) {
                    throw new ResourceNotFoundException(msg("backend.librarian.notification.memberNotFound"));
                }
                break;

            default:
                throw new ValidationException(msg("backend.librarian.notification.recipientInvalid"));
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
                msg("notification.acquisitionApproved.title"),
                msg("notification.acquisitionApproved.content", request.getTitle())
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
                msg("notification.acquisitionRejected.title"),
                msg("notification.acquisitionRejected.content", request.getTitle(), normalizedReason)
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
