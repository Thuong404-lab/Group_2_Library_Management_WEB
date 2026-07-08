package com.lms.service.impl;

import com.lms.dto.response.MemberNotificationResponse;
import com.lms.entity.*;
import com.lms.exception.ResourceNotFoundException;
import com.lms.repository.*;
import com.lms.service.MemberNotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MemberNotificationServiceImpl implements MemberNotificationService {

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
    public List<MemberNotificationResponse> getMyNotifications(String username) {
        Member member = getMemberByUsername(username);
        return memberNotificationRepository.findByMember_MemberIdOrderByNotification_CreatedDateDesc(member.getMemberId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private Member getMemberByUsername(String username) {
        MemberAccount account = memberAccountRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản: " + username));

        Member member = account.getMember();
        if (member == null) {
            throw new ResourceNotFoundException("Không tìm thấy độc giả với tài khoản: " + username);
        }
        return member;
    }

    private MemberNotificationResponse mapToResponse(MemberNotification mn) {
        MemberNotificationResponse response = new MemberNotificationResponse();
        response.setNotificationId(mn.getNotification().getNotificationId());
        response.setTitle(mn.getNotification().getTitle());
        response.setContent(mn.getNotification().getContent());
        response.setSentDate(mn.getNotification().getCreatedDate());
        response.setRead(Boolean.TRUE.equals(mn.getIsRead()));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemberNotificationResponse> getLatestNotifications(String username) {
        Member member = getMemberByUsername(username);
        return memberNotificationRepository.findTop5ByMember_MemberIdOrderByNotification_CreatedDateDesc(member.getMemberId())
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
        List<MemberNotification> unread = memberNotificationRepository.findByMember_MemberIdOrderByNotification_CreatedDateDesc(member.getMemberId())
                .stream()
                .filter(mn -> Boolean.FALSE.equals(mn.getIsRead()))
                .toList();

        if (!unread.isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            unread.forEach(mn -> {
                mn.setIsRead(true);
                mn.setReadDate(now);
            });
            memberNotificationRepository.saveAll(unread);
        }
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