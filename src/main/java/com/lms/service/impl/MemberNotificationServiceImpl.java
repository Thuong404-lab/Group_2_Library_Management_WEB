package com.lms.service.impl;

import com.lms.dto.response.MemberNotificationResponse;
import com.lms.entity.*;
import com.lms.enums.NotificationType;
import com.lms.enums.NotificationEventType;
import com.lms.enums.NotificationSource;
import com.lms.exception.ResourceNotFoundException;
import com.lms.repository.*;
import com.lms.service.MemberNotificationService;
import com.lms.service.LocalizedMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MemberNotificationServiceImpl implements MemberNotificationService {

    @Autowired
    private LocalizedMessageService messages = LocalizedMessageService.fallback();

    private final MemberAccountRepository memberAccountRepository;
    private final MemberNotificationRepository memberNotificationRepository;
    private final NotificationRepository notificationRepository;

    // Sử dụng Constructor Injection để Spring quản lý các Repository
    public MemberNotificationServiceImpl(MemberAccountRepository memberAccountRepository,
                                         MemberNotificationRepository memberNotificationRepository,
                                         NotificationRepository notificationRepository) {
        this.memberAccountRepository = memberAccountRepository;
        this.memberNotificationRepository = memberNotificationRepository;
        this.notificationRepository = notificationRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MemberNotificationResponse> getMyNotifications(String username,
                                                                NotificationSource source,
                                                                NotificationType type,
                                                                Pageable pageable) {
        Member member = getMemberByUsername(username);
        return memberNotificationRepository
                .findNotificationPage(member.getMemberId(), source, type, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public long countMyNotifications(String username) {
        return memberNotificationRepository.countByMember_MemberId(
                getMemberByUsername(username).getMemberId());
    }

    @Override
    @Transactional(readOnly = true)
    public long countMyNotificationsBySource(String username, NotificationSource source) {
        return memberNotificationRepository.countByMember_MemberIdAndNotification_NotificationSource(
                getMemberByUsername(username).getMemberId(), source);
    }

    private Member getMemberByUsername(String username) {
        MemberAccount account = memberAccountRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(messages.get("backend.profile.accountNotFound", username)));

        Member member = account.getMember();
        if (member == null) {
            throw new ResourceNotFoundException(messages.get("backend.review.memberAccountNotFound", username));
        }
        return member;
    }

    private MemberNotificationResponse mapToResponse(MemberNotification mn) {
        MemberNotificationResponse response = new MemberNotificationResponse();
        response.setNotificationId(mn.getNotification().getNotificationId());
        response.setTitle(messages.renderNotificationTitle(mn.getNotification()));
        response.setContent(messages.renderNotificationContent(mn.getNotification()));
        response.setNotificationType(mn.getNotification().getNotificationType() != null
                ? mn.getNotification().getNotificationType()
                : NotificationType.GENERAL);
        response.setSentDate(mn.getNotification().getCreatedDate());
        response.setRead(Boolean.TRUE.equals(mn.getIsRead()));
        NotificationSource source = mn.getNotification().getNotificationSource() != null
                ? mn.getNotification().getNotificationSource()
                : (mn.getNotification().getStaff() != null ? NotificationSource.LIBRARIAN : NotificationSource.SYSTEM);
        response.setNotificationSource(source);
        response.setEventType(mn.getNotification().getEventType() != null
                ? mn.getNotification().getEventType()
                : NotificationEventType.GENERAL);
        response.setFromLibrarian(source == NotificationSource.LIBRARIAN);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemberNotificationResponse> getLatestNotifications(String username) {
        Member member = getMemberByUsername(username);
        return memberNotificationRepository.findTop20ByMember_MemberIdOrderByNotification_CreatedDateDesc(member.getMemberId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnreadNotifications(String username) {
        Member member = getMemberByUsername(username);
        return memberNotificationRepository.countByMember_MemberIdAndIsReadFalse(member.getMemberId());
    }

    @Override
    @Transactional
    public void markAllNotificationsAsRead(String username) {
        Member member = getMemberByUsername(username);
        memberNotificationRepository.markUnreadNotificationsAsRead(
                member.getMemberId(), LocalDateTime.now());
    }

    @Override
    @Transactional
    public long markNotificationAsRead(String username, Integer notificationId) {
        Member member = getMemberByUsername(username);
        MemberNotificationId id = new MemberNotificationId(member.getMemberId(), notificationId);
        MemberNotification memberNotification = memberNotificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(messages.get("backend.notification.notFound")));
        if (!Boolean.TRUE.equals(memberNotification.getIsRead())) {
            memberNotification.setIsRead(true);
            memberNotification.setReadDate(LocalDateTime.now());
            memberNotificationRepository.save(memberNotification);
        }
        return memberNotificationRepository.countByMember_MemberIdAndIsReadFalse(member.getMemberId());
    }

    @Override
    @Transactional
    public void sendNotificationToUser(String username, String title, String content) {
        Member member = getMemberByUsername(username);
        Notification notification = notificationRepository.save(createNotification(title, content));

        MemberNotification memberNotification = new MemberNotification();
        memberNotification.setId(new MemberNotificationId(
                member.getMemberId(), notification.getNotificationId()));
        memberNotification.setMember(member);
        memberNotification.setNotification(notification);
        memberNotification.setIsRead(false);
        memberNotification.setReadDate(null);
        memberNotificationRepository.save(memberNotification);
    }

    // Phương thức bổ trợ để tạo notification tránh lặp code
    private Notification createNotification(String title, String content) {
        Notification n = new Notification();
        n.setTitle(title);
        n.setContent(content);
        n.setCreatedDate(LocalDateTime.now());
        n.setNotificationType(NotificationType.GENERAL);
        n.setNotificationSource(NotificationSource.SYSTEM);
        n.setEventType(NotificationEventType.GENERAL);
        n.setStatus("Active");
        return n;
    }
}
