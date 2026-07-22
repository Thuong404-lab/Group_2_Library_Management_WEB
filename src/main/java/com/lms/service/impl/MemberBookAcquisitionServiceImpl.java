package com.lms.service.impl;

import com.lms.dto.request.MemberBookAcquisitionRequest;
import com.lms.domain.AcquisitionRequestPolicy;
import com.lms.domain.IsbnUtils;
import com.lms.entity.MemberAccount;
import com.lms.entity.BookAcquisitionRequest;
import com.lms.entity.Member;
import com.lms.enums.AcquisitionRequestStatus;
import com.lms.exception.ResourceNotFoundException;
import com.lms.exception.ConflictException;
import com.lms.exception.ValidationException;
import com.lms.repository.MemberAccountRepository;
import com.lms.repository.BookAcquisitionRequestRepository;
import com.lms.repository.BookRepository;
import com.lms.service.MemberBookAcquisitionService;
import com.lms.service.LocalizedMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Year;
import java.net.URI;
import java.text.Normalizer;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;

@Service
public class MemberBookAcquisitionServiceImpl implements MemberBookAcquisitionService {

    @Autowired
    private LocalizedMessageService messages = LocalizedMessageService.fallback();

    private final MemberAccountRepository memberAccountRepository;
    private final BookAcquisitionRequestRepository bookAcquisitionRequestRepository;
    private final BookRepository bookRepository;

    public MemberBookAcquisitionServiceImpl(MemberAccountRepository memberAccountRepository,
                                            BookAcquisitionRequestRepository bookAcquisitionRequestRepository,
                                            BookRepository bookRepository) {
        this.memberAccountRepository = memberAccountRepository;
        this.bookAcquisitionRequestRepository = bookAcquisitionRequestRepository;
        this.bookRepository = bookRepository;
    }

    @Override
    @Transactional
    public void submitRequest(String username, MemberBookAcquisitionRequest request) {
        NormalizedRequest normalized = validateAndNormalize(request);
        MemberAccount account = getMemberAccount(username);
        ensureActive(account);
        Member member = account.getMember();

        if (bookAcquisitionRequestRepository.existsByMember_MemberIdAndDedupKeyAndStatusIn(
                member.getMemberId(), normalized.dedupKey(),
                AcquisitionRequestPolicy.ACTIVE_STATUSES)) {
            throw new ConflictException(messages.get("backend.acquisition.duplicate"));
        }
        ensureBookDoesNotExist(normalized);

        BookAcquisitionRequest acquisitionRequest = new BookAcquisitionRequest();
        acquisitionRequest.setMember(member);
        apply(acquisitionRequest, normalized);
        acquisitionRequest.setStatus(AcquisitionRequestStatus.PENDING);
        acquisitionRequest.setDecisionNote(null);
        acquisitionRequest.setProcessedDate(null);
        acquisitionRequest.setCreatedDate(LocalDateTime.now());

        saveWithConcurrencyHandling(acquisitionRequest);
    }

    @Override
    @Transactional
    public void updatePendingRequest(String username, Integer requestId, MemberBookAcquisitionRequest request) {
        NormalizedRequest normalized = validateAndNormalize(request);
        MemberAccount account = getMemberAccount(username);
        ensureActive(account);
        BookAcquisitionRequest existing = getOwnedRequest(account.getMember(), requestId);
        ensurePending(existing);
        if (bookAcquisitionRequestRepository.existsByMember_MemberIdAndDedupKeyAndStatusInAndRequestIdNot(
                account.getMember().getMemberId(), normalized.dedupKey(),
                AcquisitionRequestPolicy.ACTIVE_STATUSES, requestId)) {
            throw new ConflictException(messages.get("backend.acquisition.duplicate"));
        }
        ensureBookDoesNotExist(normalized);
        apply(existing, normalized);
        saveWithConcurrencyHandling(existing);
    }

    @Override
    @Transactional
    public void cancelPendingRequest(String username, Integer requestId) {
        MemberAccount account = getMemberAccount(username);
        ensureActive(account);
        BookAcquisitionRequest existing = getOwnedRequest(account.getMember(), requestId);
        ensurePending(existing);
        existing.setStatus(AcquisitionRequestStatus.CANCELLED);
        existing.setDecisionNote(null);
        existing.setProcessedBy(null);
        existing.setProcessedDate(LocalDateTime.now());
        saveWithConcurrencyHandling(existing);
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
        return value.strip().replaceAll("\\s+", " ");
    }

    private String normalizeRequired(String value, String fieldName, int minimum, int maximum) {
        String normalized = value == null ? "" : value.strip().replaceAll("\\s+", " ");
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
        if (referenceUrl.length() > AcquisitionRequestPolicy.REFERENCE_URL_MAX_LENGTH) {
            throw new ValidationException(messages.get("backend.acquisition.referenceMaximum"));
        }
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

    private NormalizedRequest validateAndNormalize(MemberBookAcquisitionRequest request) {
        String title = normalizeRequired(request.getTitle(), messages.get("book.title"),
                AcquisitionRequestPolicy.TITLE_MIN_LENGTH, AcquisitionRequestPolicy.TITLE_MAX_LENGTH);
        String author = normalizeRequired(request.getAuthor(), messages.get("book.author"),
                AcquisitionRequestPolicy.AUTHOR_MIN_LENGTH, AcquisitionRequestPolicy.AUTHOR_MAX_LENGTH);
        String reason = normalizeRequired(request.getRequestReason(), messages.get("member.acquisition.reason"),
                AcquisitionRequestPolicy.REASON_MIN_LENGTH, AcquisitionRequestPolicy.REASON_MAX_LENGTH);
        String publisher = normalizeOptional(request.getPublisher());
        if (publisher != null && publisher.length() > AcquisitionRequestPolicy.PUBLISHER_MAX_LENGTH) {
            throw new ValidationException(messages.get("backend.acquisition.publisherMaximum"));
        }
        if (publisher != null && publisher.codePoints().noneMatch(Character::isLetter)) {
            throw new ValidationException(messages.get("backend.acquisition.publisherLetters"));
        }
        String referenceUrl = request.getReferenceUrl() == null ? null : request.getReferenceUrl().strip();
        if (referenceUrl != null && referenceUrl.isEmpty()) referenceUrl = null;
        validateReferenceUrl(referenceUrl);
        Integer year = request.getPublicationYear();
        if (year != null && (year < AcquisitionRequestPolicy.MIN_PUBLICATION_YEAR
                || year > Year.now().getValue())) {
            throw new ValidationException(messages.get("validation.acquisition.year"));
        }
        String isbn = IsbnUtils.normalize(request.getIsbn());
        if (isbn != null && !IsbnUtils.isValid(isbn)) {
            throw new ValidationException(messages.get("validation.acquisition.isbnInvalid"));
        }
        String dedupKey = isbn == null
                ? "TEXT:" + keyPart(title) + "|" + keyPart(author)
                : "ISBN:" + isbn;
        return new NormalizedRequest(title, author, isbn, publisher, year, reason, referenceUrl, dedupKey);
    }

    private void ensureBookDoesNotExist(NormalizedRequest request) {
        boolean exists = request.isbn() != null
                ? bookRepository.existsByNormalizedIsbn(request.isbn())
                : bookRepository.existsByNormalizedTitleAndAuthor(request.title(), request.author());
        if (exists) throw new ConflictException(messages.get("backend.acquisition.bookAlreadyExists"));
    }

    private MemberAccount getMemberAccount(String username) {
        MemberAccount account = memberAccountRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messages.get("backend.profile.accountNotFound", username)));
        if (account.getMember() == null) {
            throw new ResourceNotFoundException(messages.get("backend.review.memberAccountNotFound", username));
        }
        return account;
    }

    private void ensureActive(MemberAccount account) {
        if (!"Active".equalsIgnoreCase(account.getStatus())) {
            throw new ConflictException(messages.get("backend.acquisition.inactiveMember"));
        }
    }

    private BookAcquisitionRequest getOwnedRequest(Member member, Integer requestId) {
        return bookAcquisitionRequestRepository.findByRequestIdAndMember_MemberId(requestId, member.getMemberId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        messages.get("backend.acquisition.notFound", requestId)));
    }

    private void ensurePending(BookAcquisitionRequest request) {
        if (request.getStatus() != AcquisitionRequestStatus.PENDING) {
            throw new ConflictException(messages.get("backend.acquisition.pendingOnly"));
        }
    }

    private void apply(BookAcquisitionRequest target, NormalizedRequest source) {
        target.setTitle(source.title());
        target.setAuthor(source.author());
        target.setIsbn(source.isbn());
        target.setPublisher(source.publisher());
        target.setPublicationYear(source.publicationYear());
        target.setRequestReason(source.reason());
        target.setReferenceUrl(source.referenceUrl());
        target.setDedupKey(source.dedupKey());
    }

    private void saveWithConcurrencyHandling(BookAcquisitionRequest request) {
        try {
            bookAcquisitionRequestRepository.saveAndFlush(request);
        } catch (OptimisticLockingFailureException exception) {
            throw new ConflictException(messages.get("backend.acquisition.dataChanged"), exception);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException(messages.get("backend.acquisition.duplicate"), exception);
        }
    }

    private String keyPart(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").strip();
    }

    private record NormalizedRequest(String title, String author, String isbn, String publisher,
                                     Integer publicationYear, String reason, String referenceUrl,
                                     String dedupKey) {
    }
}
