package com.lms.service;

import com.lms.entity.BookAcquisitionRequest;
import com.lms.entity.Member;
import com.lms.entity.Notification;
import com.lms.enums.AcquisitionRequestStatus;
import com.lms.exception.ValidationException;
import com.lms.exception.ConflictException;
import com.lms.repository.*;
import com.lms.service.impl.LibrarianInteractionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BookAcquisitionWorkflowServiceTest {

    private BookAcquisitionRequestRepository requestRepository;
    private NotificationRepository notificationRepository;
    private MemberNotificationRepository memberNotificationRepository;
    private LibrarianInteractionServiceImpl service;

    @BeforeEach
    void setUp() {
        requestRepository = mock(BookAcquisitionRequestRepository.class);
        notificationRepository = mock(NotificationRepository.class);
        memberNotificationRepository = mock(MemberNotificationRepository.class);
        service = new LibrarianInteractionServiceImpl(
                mock(FeedbackRepository.class), mock(MemberRepository.class), notificationRepository,
                memberNotificationRepository, requestRepository, mock(StaffAccountRepository.class));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            notification.setNotificationId(20);
            return notification;
        });
    }

    @Test
    void approvesPendingRequestAndNotifiesMember() {
        BookAcquisitionRequest request = pendingRequest();
        when(requestRepository.findById(1)).thenReturn(Optional.of(request));

        service.approveBookAcquisitionRequest(1);

        assertThat(request.getStatus()).isEqualTo(AcquisitionRequestStatus.APPROVED);
        assertThat(request.getDecisionNote()).isNull();
        assertThat(request.getProcessedDate()).isNotNull();
        verify(requestRepository).save(request);
        verify(memberNotificationRepository).save(any());
    }

    @Test
    void rejectRequiresReason() {
        assertThatThrownBy(() -> service.rejectBookAcquisitionRequest(1, "   "))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Lý do từ chối");
        verifyNoInteractions(requestRepository);
    }

    @Test
    void rejectionPersistsReasonAndNotifiesMember() {
        BookAcquisitionRequest request = pendingRequest();
        when(requestRepository.findById(1)).thenReturn(Optional.of(request));

        service.rejectBookAcquisitionRequest(1, "  Thông tin xuất bản chưa đầy đủ.  ");

        assertThat(request.getStatus()).isEqualTo(AcquisitionRequestStatus.REJECTED);
        assertThat(request.getDecisionNote()).isEqualTo("Thông tin xuất bản chưa đầy đủ.");
        assertThat(request.getProcessedDate()).isNotNull();
        verify(requestRepository).save(request);
        verify(memberNotificationRepository).save(any());
    }

    @Test
    void rejectRequiresAtLeastFiveCharacters() {
        assertThatThrownBy(() -> service.rejectBookAcquisitionRequest(1, "abc"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("ít nhất 5 ký tự");
        verifyNoInteractions(requestRepository);
    }

    @Test
    void rejectDoesNotAcceptOnlyNumbersOrSpecialCharacters() {
        assertThatThrownBy(() -> service.rejectBookAcquisitionRequest(1, "12345!!!"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("số hoặc ký tự đặc biệt");
        verifyNoInteractions(requestRepository);
    }

    @Test
    void cannotProcessRequestTwice() {
        BookAcquisitionRequest request = pendingRequest();
        request.setStatus(AcquisitionRequestStatus.APPROVED);
        when(requestRepository.findById(1)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> service.approveBookAcquisitionRequest(1))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("đã được xử lý");
        verify(requestRepository, never()).save(any());
    }

    private BookAcquisitionRequest pendingRequest() {
        Member member = new Member();
        member.setMemberId(7);
        BookAcquisitionRequest request = new BookAcquisitionRequest();
        request.setRequestId(1);
        request.setMember(member);
        request.setTitle("Clean Code");
        request.setStatus(AcquisitionRequestStatus.PENDING);
        return request;
    }
}
