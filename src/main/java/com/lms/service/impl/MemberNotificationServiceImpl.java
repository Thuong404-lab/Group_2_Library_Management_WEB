package com.lms.service.impl;

import com.lms.dto.response.MemberNotificationResponse;
import com.lms.entity.*;
import com.lms.enums.NotificationType;
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
    private final MemberRepository memberRepository;

    // Sử dụng Constructor Injection để Spring quản lý các Repository
    public MemberNotificationServiceImpl(MemberAccountRepository memberAccountRepository,
                                         MemberNotificationRepository memberNotificationRepository,
                                         MemberRepository memberRepository) {
        this.memberAccountRepository = memberAccountRepository;
        this.memberNotificationRepository = memberNotificationRepository;
        this.memberRepository = memberRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MemberNotificationResponse> getMyNotifications(String username, Pageable pageable) {
        Member member = getMemberByUsername(username);
        return memberNotificationRepository
                .findByMember_MemberIdOrderByNotification_CreatedDateDesc(member.getMemberId(), pageable)
                .map(this::mapToResponse);
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
        response.setTitle(mn.getNotification().getTitle());
        response.setContent(mn.getNotification().getContent());
        response.setNotificationType(mn.getNotification().getNotificationType() != null
                ? mn.getNotification().getNotificationType()
                : NotificationType.GENERAL);
        response.setSentDate(mn.getNotification().getCreatedDate());
        response.setRead(Boolean.TRUE.equals(mn.getIsRead()));
        response.setFromLibrarian(mn.getNotification().getStaff() != null);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemberNotificationResponse> getAllMyNotifications(String username) {
        Member member = getMemberByUsername(username);
        return memberNotificationRepository
                .findByMember_MemberIdOrderByNotification_CreatedDateDesc(member.getMemberId())
                .stream().map(this::mapToResponse).toList();
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
        memberRepository.findByAccountUsername(username).ifPresent(member -> {
            Notification notification = createNotification(title, content);
            MemberNotification mn = new MemberNotification();
            mn.setMember(member);
            mn.setNotification(notification);
            mn.setIsRead(false);
            memberNotificationRepository.save(mn);
        });
    }

    @Override
    @Transactional
    public void sendNotificationToAllLibrarians(String title, String content) {
        // Lấy tất cả tài khoản, lọc theo Role và gửi thông báo
        memberAccountRepository.findAll().forEach(acc -> {
            boolean isLibrarian = acc.getRoles().stream()
                    .anyMatch(role -> role.getName() != null && role.getName().toUpperCase().contains("LIBRARIAN"));

            if (isLibrarian && acc.getMember() != null) {
                Notification notification = createNotification(title, content);
                MemberNotification mn = new MemberNotification();
                mn.setMember(acc.getMember());
                mn.setNotification(notification);
                mn.setIsRead(false);
                memberNotificationRepository.save(mn);
            }
        });
    }

    // Phương thức bổ trợ để tạo notification tránh lặp code
    private Notification createNotification(String title, String content) {
        Notification n = new Notification();
        n.setTitle(title);
        n.setContent(content);
        n.setCreatedDate(LocalDateTime.now());
        return n;
    }
}
