package com.lms.service.impl;

import com.lms.dto.response.MemberNotificationResponse;
import com.lms.entity.Account;
import com.lms.entity.Member;
import com.lms.entity.MemberNotification;
import com.lms.exception.ResourceNotFoundException;
import com.lms.repository.AccountRepository;
import com.lms.repository.MemberNotificationRepository;
import com.lms.repository.MemberRepository;
import com.lms.service.MemberNotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MemberNotificationServiceImpl implements MemberNotificationService {

    private final AccountRepository accountRepository;
    private final MemberRepository memberRepository;
    private final MemberNotificationRepository memberNotificationRepository;

    public MemberNotificationServiceImpl(AccountRepository accountRepository,
                                         MemberRepository memberRepository,
                                         MemberNotificationRepository memberNotificationRepository) {
        this.accountRepository = accountRepository;
        this.memberRepository = memberRepository;
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
        return memberRepository.findByAccountUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy độc giả với tài khoản: " + username
                ));
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

        // Ngày gửi thông báo
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
    public void deleteNotificationById(Integer memberNotificationId, String username) {
        Member member = getMemberByUsername(username);

        // MemberNotificationId = (memberId, notificationId) -> ta dùng notificationId làm input.
        // Truy vấn theo memberId + notificationId sẽ xóa đúng bản ghi.
        memberNotificationRepository
                .findByMemberMemberIdAndNotificationId(member.getMemberId(), memberNotificationId)
                .ifPresentOrElse(
                        memberNotificationRepository::delete,
                        () -> {
                            // không cần throw để tránh UX xấu khi id không tồn tại
                        }
                );
    }

    @Override
    @Transactional
    public void deleteAllNotifications(String username) {
        Member member = getMemberByUsername(username);
        List<MemberNotification> notifications = memberNotificationRepository
                .findByMember_MemberIdOrderByNotification_CreatedDateDesc(member.getMemberId());
        if (notifications == null || notifications.isEmpty()) {
            return;
        }
        memberNotificationRepository.deleteAll(notifications);
    }
}

