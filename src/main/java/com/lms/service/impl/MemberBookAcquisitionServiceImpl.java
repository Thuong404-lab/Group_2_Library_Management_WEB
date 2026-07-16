package com.lms.service.impl;

import com.lms.dto.request.MemberBookAcquisitionRequest;
import com.lms.entity.MemberAccount;
import com.lms.entity.BookAcquisitionRequest;
import com.lms.entity.Member;
import com.lms.enums.AcquisitionRequestStatus;
import com.lms.exception.ResourceNotFoundException;
import com.lms.exception.ValidationException;
import com.lms.repository.MemberAccountRepository;
import com.lms.repository.BookAcquisitionRequestRepository;
import com.lms.service.MemberBookAcquisitionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
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
        MemberAccount account = memberAccountRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản: " + username));

        Member member = account.getMember();
        if (member == null) {
            throw new ResourceNotFoundException("Không tìm thấy độc giả với tài khoản: " + username);
        }

        String title = request.getTitle().trim();

        if (bookAcquisitionRequestRepository.existsByMember_MemberIdAndTitleIgnoreCaseAndStatusIn(
                member.getMemberId(), title,
                List.of(AcquisitionRequestStatus.PENDING, AcquisitionRequestStatus.APPROVED))) {
            throw new ValidationException("Bạn đã đề xuất sách này rồi.");
        }

        if (request.getPublicationYear() != null && request.getPublicationYear() > Year.now().getValue()) {
            throw new ValidationException("Năm xuất bản không được lớn hơn năm hiện tại.");
        }

        BookAcquisitionRequest acquisitionRequest = new BookAcquisitionRequest();
        acquisitionRequest.setMember(member);
        acquisitionRequest.setTitle(title);
        acquisitionRequest.setAuthor(request.getAuthor().trim());
        acquisitionRequest.setIsbn(normalizeOptional(request.getIsbn()));
        acquisitionRequest.setPublisher(normalizeOptional(request.getPublisher()));
        acquisitionRequest.setPublicationYear(request.getPublicationYear());
        acquisitionRequest.setRequestReason(request.getRequestReason().trim());
        acquisitionRequest.setReferenceUrl(normalizeOptional(request.getReferenceUrl()));
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
}
