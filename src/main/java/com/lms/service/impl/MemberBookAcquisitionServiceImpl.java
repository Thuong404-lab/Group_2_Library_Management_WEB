package com.lms.service.impl;

import com.lms.dto.request.MemberBookAcquisitionRequest;
import com.lms.entity.MemberAccount;
import com.lms.entity.BookAcquisitionRequest;
import com.lms.entity.Member;
import com.lms.enums.AcquisitionRequestStatus;
import com.lms.exception.ResourceNotFoundException;
import com.lms.exception.ConflictException;
import com.lms.exception.ValidationException;
import com.lms.repository.MemberAccountRepository;
import com.lms.repository.BookAcquisitionRequestRepository;
import com.lms.service.MemberBookAcquisitionService;
import com.lms.service.LocalizedMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Year;
import java.net.URI;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
public class MemberBookAcquisitionServiceImpl implements MemberBookAcquisitionService {

    @Autowired
    private LocalizedMessageService messages = LocalizedMessageService.fallback();

    private final MemberAccountRepository memberAccountRepository;
    private final BookAcquisitionRequestRepository bookAcquisitionRequestRepository;

    public MemberBookAcquisitionServiceImpl(MemberAccountRepository memberAccountRepository,
                                            BookAcquisitionRequestRepository bookAcquisitionRequestRepository) {
        this.memberAccountRepository = memberAccountRepository;
        this.bookAcquisitionRequestRepository = bookAcquisitionRequestRepository;
    }

    @Override
    @Transactional
    public void submitRequest(String username, MemberBookAcquisitionRequest request) {
        String title = normalizeRequired(request.getTitle(), messages.get("book.title"), 2, 255);
        String author = normalizeRequired(request.getAuthor(), messages.get("book.author"), 2, 255);
        String reason = normalizeRequired(request.getRequestReason(), messages.get("member.acquisition.reason"), 10, 1000);
        String publisher = normalizeOptional(request.getPublisher());
        String referenceUrl = normalizeOptional(request.getReferenceUrl());
        if (publisher != null && publisher.length() > 255) {
            throw new ValidationException(messages.get("backend.acquisition.publisherMaximum"));
        }
        if (publisher != null && publisher.codePoints().noneMatch(Character::isLetter)) {
            throw new ValidationException(messages.get("backend.acquisition.publisherLetters"));
        }
        validateReferenceUrl(referenceUrl);

        MemberAccount account = memberAccountRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(messages.get("backend.profile.accountNotFound", username)));

        Member member = account.getMember();
        if (member == null) {
            throw new ResourceNotFoundException(messages.get("backend.review.memberAccountNotFound", username));
        }

        if (bookAcquisitionRequestRepository.existsByMember_MemberIdAndTitleIgnoreCaseAndStatusIn(
                member.getMemberId(), title,
                List.of(AcquisitionRequestStatus.PENDING, AcquisitionRequestStatus.APPROVED))) {
            throw new ConflictException(messages.get("backend.acquisition.duplicate"));
        }

        if (request.getPublicationYear() != null && request.getPublicationYear() > Year.now().getValue()) {
            throw new ValidationException(messages.get("backend.acquisition.futureYear"));
        }

        BookAcquisitionRequest acquisitionRequest = new BookAcquisitionRequest();
        acquisitionRequest.setMember(member);
        acquisitionRequest.setTitle(title);
        acquisitionRequest.setAuthor(author);
        acquisitionRequest.setIsbn(normalizeOptional(request.getIsbn()));
        acquisitionRequest.setPublisher(publisher);
        acquisitionRequest.setPublicationYear(request.getPublicationYear());
        acquisitionRequest.setRequestReason(reason);
        acquisitionRequest.setReferenceUrl(referenceUrl);
        acquisitionRequest.setStatus(AcquisitionRequestStatus.PENDING);
        acquisitionRequest.setDecisionNote(null);
        acquisitionRequest.setProcessedDate(null);
        acquisitionRequest.setCreatedDate(LocalDateTime.now());

        bookAcquisitionRequestRepository.save(acquisitionRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BookAcquisitionRequest> getMyRequests(String username, Pageable pageable) {
        Member member = memberAccountRepository.findByUsername(username)
                .map(MemberAccount::getMember)
                .orElseThrow(() -> new ResourceNotFoundException(messages.get("backend.member.currentNotFound")));
        return bookAcquisitionRequestRepository.findByMember_MemberIdOrderByCreatedDateDesc(
                member.getMemberId(), pageable);
    }

    private String normalizeOptional(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeRequired(String value, String fieldName, int minimum, int maximum) {
        String normalized = value == null ? "" : value.trim().replaceAll("\\s+", " ");
        if (normalized.isEmpty()) throw new ValidationException(messages.get("validation.fieldRequired", fieldName));
        if (normalized.length() < minimum || normalized.length() > maximum) {
            throw new ValidationException(messages.get("validation.fieldRange", fieldName, minimum, maximum));
        }
        if (normalized.codePoints().noneMatch(Character::isLetter)) {
            throw new ValidationException(messages.get("validation.fieldLetters", fieldName));
        }
        return normalized;
    }

    private void validateReferenceUrl(String referenceUrl) {
        if (referenceUrl == null) return;
        if (referenceUrl.length() > 500) throw new ValidationException(messages.get("backend.acquisition.referenceMaximum"));
        try {
            URI uri = URI.create(referenceUrl);
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null || uri.getHost().isBlank()) {
                throw new ValidationException(messages.get("validation.httpUrl"));
            }
        } catch (IllegalArgumentException ex) {
            throw new ValidationException(messages.get("validation.httpUrl"));
        }
    }
}
