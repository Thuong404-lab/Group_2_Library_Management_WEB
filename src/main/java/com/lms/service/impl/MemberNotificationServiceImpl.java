package com.lms.service.impl;

import com.lms.dto.response.MemberNotificationResponse;
import com.lms.entity.MemberAccount;
import com.lms.entity.Member;
import com.lms.entity.MemberNotification;
import com.lms.entity.Notification;
import com.lms.exception.ResourceNotFoundException;
import com.lms.repository.MemberAccountRepository;
import com.lms.repository.MemberNotificationRepository;
import com.lms.service.MemberNotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MemberNotificationServiceImpl implements MemberNotificationService {

    private final MemberAccountRepository memberAccountRepository;
    private final MemberNotificationRepository memberNotificationRepository;

    public MemberNotificationServiceImpl(MemberAccountRepository memberAccountRepository,
                                         MemberNotificationRepository memberNotificationRepository) {
        this.memberAccountRepository = memberAccountRepository;
        this.memberNotificationRepository = memberNotificationRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemberNotificationResponse> getMyNotifications(String username) {
        Member member = getMemberByUsername(username);

        List<MemberNotification> memberNotifications =
                memberNotificationRepository.findByMember_MemberIdOrderByNotification_CreatedDateDesc(
                        member.getMemberId()
                );

        return memberNotifications.stream()
                .map(this::mapToResponse)
                .toList();
    }

    private Member getMemberByUsername(String username) {
        MemberAccount account = memberAccountRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy tài khoản: " + username
                ));

        Member member = account.getMember();
        if (member == null) {
            throw new ResourceNotFoundException("Không tìm thấy độc giả với tài khoản: " + username);
        }
        return member;
    }

    private MemberNotificationResponse mapToResponse(MemberNotification memberNotification) {
        MemberNotificationResponse response = new MemberNotificationResponse();

        response.setNotificationId(
                memberNotification.getNotification().getNotificationId()
        );

        response.setTitle(
                memberNotification.getNotification().getTitle()
        );

        response.setContent(
                memberNotification.getNotification().getContent()
        );

        response.setSentDate(
                memberNotification.getNotification().getCreatedDate()
        );
        response.setRead(Boolean.TRUE.equals(memberNotification.getIsRead()));

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemberNotificationResponse> getLatestNotifications(String username) {
        Member member = getMemberByUsername(username);

        List<MemberNotification> memberNotifications =
                memberNotificationRepository
                        .findTop5ByMember_MemberIdOrderByNotification_CreatedDateDesc(
                                member.getMemberId()
                        );

        return memberNotifications.stream()
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

        List<MemberNotification> unreadNotifications = memberNotificationRepository
                .findByMember_MemberIdOrderByNotification_CreatedDateDesc(member.getMemberId())
                .stream()
                .filter(mn -> Boolean.FALSE.equals(mn.getIsRead()))
                .toList();

        if (!unreadNotifications.isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            unreadNotifications.forEach(mn -> {
                mn.setIsRead(true);
                mn.setReadDate(now);
            });
            memberNotificationRepository.saveAll(unreadNotifications);
        }
    }

    @Override
    @Transactional
    public void sendNotificationToUser(String username, String title, String content) {
        Member member = memberRepository.findByAccountUsername(username).orElse(null);
        if (member == null) return;

        Notification notification = new Notification();
        notification.setTitle(title);
        notification.setContent(content);
        notification.setCreatedDate(LocalDateTime.now());

        MemberNotification memberNotification = new MemberNotification();
        memberNotification.setMember(member);
        memberNotification.setNotification(notification);
        memberNotification.setIsRead(false);

        memberNotificationRepository.save(memberNotification);
    }

    // ======= ĐÃ FIX LỖI: Stream qua Collection Set<Role> để tìm quyền LIBRARIAN =======
    @Override
    @Transactional
    public void sendNotificationToAllLibrarians(String title, String content) {
        List<Account> librarians = accountRepository.findAll().stream()
                .filter(acc -> acc.getRoles() != null && acc.getRoles().stream()
                        .anyMatch(role -> {
                            // Giả định thực thể Role của bạn có method getName() hoặc dùng toString()
                            // Bạn hãy đổi thành .getRoleName() nếu thuộc tính trong class Role mang tên khác nhé
                            String rName = (role.toString() != null) ? role.toString() : "";
                            return rName.toUpperCase().contains("LIBRARIAN");
                        }))
                .toList();

        for (Account acc : librarians) {
            memberRepository.findByAccountUsername(acc.getUsername()).ifPresent(member -> {
                Notification notification = new Notification();
                notification.setTitle(title);
                notification.setContent(content);
                notification.setCreatedDate(LocalDateTime.now());

                MemberNotification mn = new MemberNotification();
                mn.setMember(member);
                mn.setNotification(notification);
                mn.setIsRead(false);
                memberNotificationRepository.save(mn);
            });
        }
    }
}