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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Year;
import java.net.URI;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
public class MemberBookAcquisitionServiceImpl implements MemberBookAcquisitionService {

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
        String title = normalizeRequired(request.getTitle(), "Tên sách", 2, 255);
        String author = normalizeRequired(request.getAuthor(), "Tên tác giả", 2, 255);
        String reason = normalizeRequired(request.getRequestReason(), "Lý do đề xuất", 10, 1000);
        String publisher = normalizeOptional(request.getPublisher());
        String referenceUrl = normalizeOptional(request.getReferenceUrl());
        if (publisher != null && publisher.length() > 255) {
            throw new ValidationException("Nhà xuất bản không được vượt quá 255 ký tự.");
        }
        if (publisher != null && publisher.codePoints().noneMatch(Character::isLetter)) {
            throw new ValidationException("Nhà xuất bản không được chỉ gồm số hoặc ký tự đặc biệt.");
        }
        validateReferenceUrl(referenceUrl);

        MemberAccount account = memberAccountRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản: " + username));

        Member member = account.getMember();
        if (member == null) {
            throw new ResourceNotFoundException("Không tìm thấy độc giả với tài khoản: " + username);
        }

        if (bookAcquisitionRequestRepository.existsByMember_MemberIdAndTitleIgnoreCaseAndStatusIn(
                member.getMemberId(), title,
                List.of(AcquisitionRequestStatus.PENDING, AcquisitionRequestStatus.APPROVED))) {
            throw new ConflictException("Bạn đã đề xuất sách này rồi.");
        }

        if (request.getPublicationYear() != null && request.getPublicationYear() > Year.now().getValue()) {
            throw new ValidationException("Năm xuất bản không được lớn hơn năm hiện tại.");
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
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy member."));
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
        if (normalized.isEmpty()) throw new ValidationException(fieldName + " không được để trống.");
        if (normalized.length() < minimum || normalized.length() > maximum) {
            String maximumLabel = NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi")).format(maximum);
            throw new ValidationException(fieldName + " phải có từ " + minimum + " đến " + maximumLabel + " ký tự.");
        }
        if (normalized.codePoints().noneMatch(Character::isLetter)) {
            throw new ValidationException(fieldName + " không được chỉ gồm số hoặc ký tự đặc biệt.");
        }
        return normalized;
    }

    private void validateReferenceUrl(String referenceUrl) {
        if (referenceUrl == null) return;
        if (referenceUrl.length() > 500) throw new ValidationException("Link tham khảo không được vượt quá 500 ký tự.");
        try {
            URI uri = URI.create(referenceUrl);
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null || uri.getHost().isBlank()) {
                throw new ValidationException("Giao thức URL không hợp lệ.");
            }
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("Link tham khảo phải là địa chỉ HTTP hoặc HTTPS hợp lệ.");
        }
    }
}
