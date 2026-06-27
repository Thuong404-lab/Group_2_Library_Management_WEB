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
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy tài khoản: " + username
                ));

        return memberRepository.findByUserId(account.getUser().getId())
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
}