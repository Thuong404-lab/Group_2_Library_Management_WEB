package com.lms.service.impl;

import com.lms.dto.request.MemberBookAcquisitionRequest;
import com.lms.entity.BookAcquisitionRequest;
import com.lms.entity.Member;
import com.lms.entity.MemberAccount;
import com.lms.enums.AcquisitionRequestStatus;
import com.lms.exception.ConflictException;
import com.lms.exception.ResourceNotFoundException;
import com.lms.exception.ValidationException;
import com.lms.repository.BookAcquisitionRequestRepository;
import com.lms.repository.BookRepository;
import com.lms.repository.MemberAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberBookAcquisitionServiceImplTest {

    @Mock private MemberAccountRepository memberAccountRepository;
    @Mock private BookAcquisitionRequestRepository acquisitionRequestRepository;
    @Mock private BookRepository bookRepository;

    private MemberBookAcquisitionServiceImpl service;
    private Member member;

    @BeforeEach
    void setUp() {
        service = new MemberBookAcquisitionServiceImpl(
                memberAccountRepository, acquisitionRequestRepository, bookRepository);
        member = new Member();
        member.setMemberId(7);
        MemberAccount account = new MemberAccount();
        account.setUsername("reader");
        account.setStatus("Active");
        account.setMember(member);
        lenient().when(memberAccountRepository.findByUsername("reader")).thenReturn(Optional.of(account));
    }

    @Test
    void submitNormalizesDataAndCreatesAuditablePendingRequest() {
        MemberBookAcquisitionRequest input = validRequest();
        input.setTitle("  Effective   Java  ");
        input.setIsbn("978-0-13-468599-1");

        service.submitRequest("reader", input);

        ArgumentCaptor<BookAcquisitionRequest> captor = ArgumentCaptor.forClass(BookAcquisitionRequest.class);
        verify(acquisitionRequestRepository).saveAndFlush(captor.capture());
        BookAcquisitionRequest saved = captor.getValue();
        assertEquals("Effective Java", saved.getTitle());
        assertEquals("9780134685991", saved.getIsbn());
        assertEquals("ISBN:9780134685991", saved.getDedupKey());
        assertEquals(AcquisitionRequestStatus.PENDING, saved.getStatus());
        assertNotNull(saved.getCreatedDate());
        assertNull(saved.getProcessedDate());
        assertNull(saved.getDecisionNote());
    }

    @Test
    void rejectsInvalidIsbnBeforeWriting() {
        MemberBookAcquisitionRequest input = validRequest();
        input.setIsbn("9780134685992");

        assertThrows(ValidationException.class, () -> service.submitRequest("reader", input));

        verify(acquisitionRequestRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsBookThatAlreadyExistsInCatalog() {
        MemberBookAcquisitionRequest input = validRequest();
        when(bookRepository.existsByNormalizedTitleAndAuthor("Effective Java", "Joshua Bloch"))
                .thenReturn(true);

        assertThrows(ConflictException.class, () -> service.submitRequest("reader", input));

        verify(acquisitionRequestRepository, never()).saveAndFlush(any());
    }

    @Test
    void onlyOwnerCanCancelPendingRequest() {
        when(acquisitionRequestRepository.findByRequestIdAndMember_MemberId(4, 7))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.cancelPendingRequest("reader", 4));

        verify(acquisitionRequestRepository, never()).saveAndFlush(any());
    }

    @Test
    void cancellingPendingRequestKeepsHistoryAndReleasesActiveDeduplication() {
        BookAcquisitionRequest existing = new BookAcquisitionRequest();
        existing.setRequestId(4);
        existing.setMember(member);
        existing.setStatus(AcquisitionRequestStatus.PENDING);
        existing.setDedupKey("TEXT:effective java|joshua bloch");
        when(acquisitionRequestRepository.findByRequestIdAndMember_MemberId(4, 7))
                .thenReturn(Optional.of(existing));

        service.cancelPendingRequest("reader", 4);

        assertEquals(AcquisitionRequestStatus.CANCELLED, existing.getStatus());
        assertNotNull(existing.getProcessedDate());
        assertNull(existing.getProcessedBy());
        verify(acquisitionRequestRepository).saveAndFlush(existing);
    }

    @Test
    void processedRequestCannotBeEdited() {
        BookAcquisitionRequest existing = new BookAcquisitionRequest();
        existing.setRequestId(4);
        existing.setMember(member);
        existing.setStatus(AcquisitionRequestStatus.APPROVED);
        when(acquisitionRequestRepository.findByRequestIdAndMember_MemberId(4, 7))
                .thenReturn(Optional.of(existing));

        assertThrows(ConflictException.class,
                () -> service.updatePendingRequest("reader", 4, validRequest()));

        verify(acquisitionRequestRepository, never()).saveAndFlush(any());
    }

    private MemberBookAcquisitionRequest validRequest() {
        MemberBookAcquisitionRequest request = new MemberBookAcquisitionRequest();
        request.setTitle("Effective Java");
        request.setAuthor("Joshua Bloch");
        request.setRequestReason("A valuable reference for Java developers.");
        request.setReferenceUrl("https://example.com/books/effective-java");
        return request;
    }
}
