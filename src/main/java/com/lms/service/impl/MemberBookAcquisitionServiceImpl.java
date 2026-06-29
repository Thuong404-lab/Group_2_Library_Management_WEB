package com.lms.service.impl;

import com.lms.dto.request.MemberBookAcquisitionRequest;
import com.lms.entity.Account;
import com.lms.entity.BookAcquisitionRequest;
import com.lms.entity.Member;
import com.lms.exception.ResourceNotFoundException;
import com.lms.exception.ValidationException;
import com.lms.repository.AccountRepository;
import com.lms.repository.BookAcquisitionRequestRepository;
import com.lms.repository.MemberRepository;
import com.lms.service.MemberBookAcquisitionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class MemberBookAcquisitionServiceImpl implements MemberBookAcquisitionService {

    private final AccountRepository accountRepository;
    private final MemberRepository memberRepository;
    private final BookAcquisitionRequestRepository bookAcquisitionRequestRepository;

    public MemberBookAcquisitionServiceImpl(AccountRepository accountRepository,
                                            MemberRepository memberRepository,
                                            BookAcquisitionRequestRepository bookAcquisitionRequestRepository) {
        this.accountRepository = accountRepository;
        this.memberRepository = memberRepository;
        this.bookAcquisitionRequestRepository = bookAcquisitionRequestRepository;
    }

    @Override
    @Transactional
    public void submitRequest(String username, MemberBookAcquisitionRequest request) {
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản: " + username));

        Member member = memberRepository.findByUserId(account.getUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy độc giả với tài khoản: " + username));

        String title = request.getTitle().trim();

        if (bookAcquisitionRequestRepository.existsByMember_MemberIdAndTitleIgnoreCase(
                member.getMemberId(), title)) {
            throw new ValidationException("Bạn đã đề xuất sách này rồi.");
        }

        BookAcquisitionRequest acquisitionRequest = new BookAcquisitionRequest();
        acquisitionRequest.setMember(member);
        acquisitionRequest.setTitle(title);
        acquisitionRequest.setAuthor(request.getAuthor() == null ? null : request.getAuthor().trim());
        acquisitionRequest.setCreatedDate(LocalDateTime.now());

        bookAcquisitionRequestRepository.save(acquisitionRequest);
    }
}