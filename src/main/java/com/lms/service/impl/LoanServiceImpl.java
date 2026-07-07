package com.lms.service.impl;

import com.lms.entity.*;
import com.lms.repository.*;
import com.lms.service.LoanService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * LoanService - Xử lý Logic Quản lý Phiếu mượn (Thủ thư)
 * Người phụ trách: Huỳnh Gia Hưng (CE190488)
 */
@Service
public class LoanServiceImpl implements LoanService {
    private final BorrowRepository borrowRepository;
    private final BorrowDetailRepository borrowDetailRepository;
    private final MemberRepository memberRepository;
    private final BookItemRepository bookItemRepository;
    private final StaffAccountRepository staffAccountRepository;

    public LoanServiceImpl(BorrowRepository borrowRepository,
                           BorrowDetailRepository borrowDetailRepository,
                           MemberRepository memberRepository,
                           BookItemRepository bookItemRepository,
                           StaffAccountRepository staffAccountRepository) {
        this.borrowRepository = borrowRepository;
        this.borrowDetailRepository = borrowDetailRepository;
        this.memberRepository = memberRepository;
        this.bookItemRepository = bookItemRepository;
        this.staffAccountRepository = staffAccountRepository;
    }

    // UC-13.1: Xem chi tiết phiếu mượn
    @Override
    public void getLoanDetails(Integer borrowId) {
        // TODO: Implement
    }

    // UC-13.2: Xác nhận trả sách
    @Override
    public void confirmReturn(String barcode, Integer memberId) {
        // TODO: Implement
    }

    // UC-13.3: Quầy mượn sách
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processBorrowDesk(String memberIdentifier, List<String> barcodes, String staffUsername) throws Exception {
        Member member = null;
        try {
            if (memberIdentifier != null && memberIdentifier.contains("@")) {
                member = memberRepository.findByUserEmail(memberIdentifier)
                        .orElseThrow(() -> new Exception("Không tìm thấy thành viên với Email này!"));
            } else {
                member = memberRepository.findByUserPhone(memberIdentifier)
                        .orElseThrow(() -> new Exception("Không tìm thấy thành viên với số điện thoại này!"));
            }
        } catch (org.springframework.dao.IncorrectResultSizeDataAccessException e) {
            throw new Exception("Có nhiều độc giả trùng thông tin này! Vui lòng liên hệ quản trị viên để xử lý dữ liệu trùng lặp.");
        }

        Staff staff = staffAccountRepository.findByUsername(staffUsername)
                .map(StaffAccount::getStaff)
                .orElseThrow(() -> new Exception("Không tìm thấy thông tin thủ thư!"));

        List<BookItem> bookItemsToBorrow = new ArrayList<>();
        for (String barcode : barcodes) {
            String trimmedBarcode = barcode.trim();
            if (trimmedBarcode.isEmpty()) continue;

            BookItem item = null;
            try {
                item = bookItemRepository.findByBarcode(trimmedBarcode)
                        .orElseThrow(() -> new Exception("Mã vạch " + trimmedBarcode + " không tồn tại!"));
            } catch (org.springframework.dao.IncorrectResultSizeDataAccessException e) {
                throw new Exception("Lỗi CSDL: Có nhiều cuốn sách cùng sử dụng mã vạch " + trimmedBarcode + ". Vui lòng liên hệ Admin!");
            }

            if (!"Available".equalsIgnoreCase(item.getStatus())) {
                throw new Exception("Sách có mã vạch " + trimmedBarcode + " hiện tại không sẵn sàng!");
            }
            bookItemsToBorrow.add(item);
        }

        if (bookItemsToBorrow.isEmpty()) {
            throw new Exception("Vui lòng nhập ít nhất 1 mã vạch hợp lệ!");
        }

        long currentBorrowCount = borrowDetailRepository.countActiveBorrowedBooks(member.getMemberId());
        int maxLimit = member.getTier() != null ? member.getTier().getBorrowLimit() : 3;
        int totalRequestedBooks = (int) currentBorrowCount + bookItemsToBorrow.size();

        if (totalRequestedBooks > maxLimit) {
            throw new Exception("Số lượng sách vượt quá giới hạn mượn của thành viên! (Tối đa " + maxLimit + " cuốn, đang mượn " + currentBorrowCount + " cuốn)");
        }

        Borrow borrow = new Borrow();
        borrow.setMember(member);
        borrow.setStaff(staff);
        borrow.setBorrowDate(LocalDateTime.now());
        borrow.setStatus("Active");
        borrow = borrowRepository.save(borrow);

        for (BookItem item : bookItemsToBorrow) {
            BorrowDetail detail = new BorrowDetail();
            detail.setBorrow(borrow);
            detail.setBook(item.getBook());
            detail.setBookItem(item);
            detail.setDueDate(LocalDateTime.now().plusDays(14));
            detail.setStatus("Borrowed");
            detail.setRenewCount(0);
            borrowDetailRepository.save(detail);

            item.setStatus("Borrowed");
            bookItemRepository.save(item);
        }
    }

    // UC-13.4: Gia hạn mượn
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processRenewal(Integer borrowDetailId) throws Exception {
        BorrowDetail detail = borrowDetailRepository.findById(borrowDetailId)
                .orElseThrow(() -> new Exception("Không tìm thấy chi tiết mượn sách này!"));

        if (!"Borrowed".equalsIgnoreCase(detail.getStatus())) {
            throw new Exception("Chỉ có thể gia hạn những sách đang mượn!");
        }

        if (detail.getRenewCount() >= 2) {
            throw new Exception("Sách này đã vượt quá số lần gia hạn cho phép (tối đa 2 lần)!");
        }

        detail.setDueDate(detail.getDueDate().plusDays(7));
        detail.setRenewCount(detail.getRenewCount() + 1);
        borrowDetailRepository.save(detail);
    }

    @Override
    public java.util.List<com.lms.entity.BorrowDetail> getAllBorrowDetails() {
        return borrowDetailRepository.findAll();
    }
}
