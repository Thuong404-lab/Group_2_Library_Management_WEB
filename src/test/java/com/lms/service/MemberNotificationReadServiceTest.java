package com.lms.service;

import com.lms.entity.Member;
import com.lms.entity.MemberAccount;
import com.lms.entity.MemberNotification;
import com.lms.entity.MemberNotificationId;
import com.lms.repository.MemberAccountRepository;
import com.lms.repository.MemberNotificationRepository;
import com.lms.repository.MemberRepository;
import com.lms.service.impl.MemberNotificationServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class MemberNotificationReadServiceTest {

    @Test
    void marksOnlySelectedNotificationAndReturnsRemainingUnreadCount() {
        MemberAccountRepository accountRepository = mock(MemberAccountRepository.class);
        MemberNotificationRepository notificationRepository = mock(MemberNotificationRepository.class);
        MemberNotificationServiceImpl service = new MemberNotificationServiceImpl(
                accountRepository, notificationRepository, mock(MemberRepository.class));

        Member member = new Member();
        member.setMemberId(7);
        MemberAccount account = new MemberAccount();
        account.setMember(member);
        MemberNotification selected = new MemberNotification();
        selected.setIsRead(false);
        MemberNotificationId selectedId = new MemberNotificationId(7, 21);

        when(accountRepository.findByUsername("member7")).thenReturn(Optional.of(account));
        when(notificationRepository.findById(selectedId)).thenReturn(Optional.of(selected));
        when(notificationRepository.countByMember_MemberIdAndIsReadFalse(7)).thenReturn(12L);

        long unreadCount = service.markNotificationAsRead("member7", 21);

        assertThat(selected.getIsRead()).isTrue();
        assertThat(selected.getReadDate()).isNotNull();
        assertThat(unreadCount).isEqualTo(12L);
        verify(notificationRepository).save(selected);
        verify(notificationRepository, never()).markUnreadNotificationsAsRead(anyInt(), any());
    }
}
