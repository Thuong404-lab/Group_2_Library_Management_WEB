package com.lms.service.impl;

import com.lms.dto.request.BorrowRequest;
import com.lms.entity.*;
import com.lms.enums.UserStatus;
import com.lms.repository.*;
import com.lms.service.BorrowService;
import com.lms.service.FinancialService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BorrowServiceImpl implements BorrowService {

    private final MemberRepository memberRepository;
    private final BookItemRepository bookItemRepository;
    private final BorrowRepository borrowRepository;
    private final BorrowDetailRepository borrowDetailRepository;
    private final BookRepository bookRepository;
    private final AccountRepository accountRepository;
    private final FinancialService financialService;

    public BorrowServiceImpl(MemberRepository memberRepository,
                             BookItemRepository bookItemRepository,
                             BorrowRepository borrowRepository,
                             BorrowDetailRepository borrowDetailRepository,
                             BookRepository bookRepository,
                             AccountRepository accountRepository,
                             FinancialService financialService) {
        this.memberRepository = memberRepository;
        this.bookItemRepository = bookItemRepository;
        this.borrowRepository = borrowRepository;
        this.borrowDetailRepository = borrowDetailRepository;
        this.bookRepository = bookRepository;
        this.accountRepository = accountRepository;
        this.financialService = financialService;
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public Borrow processBorrowing(BorrowRequest request, String librarianUsername) throws Exception {
        Member member = memberRepository.findByUserEmail(request.getMemberEmail())
                .orElseThrow(() -> new Exception("Không tìm thấy độc giả với email này!"));

        if (member.getUser() != null && member.getUser().getStatus() != UserStatus.Active) {
            throw new Exception("Tài khoản thành viên này đang bị khóa hoặc chưa kích hoạt!");
        }

        List<BookItem> bookItemsToBorrow = new ArrayList<>();
        for (String barcode : request.getBarcodes()) {
            BookItem item = bookItemRepository.findByBarcode(barcode)
                    .orElseThrow(() -> new Exception("Mã vạch " + barcode + " không tồn tại!"));

            if (!"Available".equalsIgnoreCase(item.getStatus())) {
                throw new Exception("Sách có mã vạch " + barcode + " hiện tại không sẵn sàng!");
            }
            bookItemsToBorrow.add(item);
        }

        long currentBorrowCount = borrowDetailRepository.countActiveBorrowedBooks(member.getMemberId());
        int maxLimit = member.getTier() != null ? member.getTier().getBorrowLimit() : 3;
        int totalRequestedBooks = (int) currentBorrowCount + bookItemsToBorrow.size();

        if (totalRequestedBooks > maxLimit) {
            throw new Exception("Số lượng sách vượt quá giới hạn mượn của hạng thành viên!");
        }

        Borrow borrow = new Borrow();
        borrow.setMember(member);
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

        return borrow;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Borrow memberSubmitBorrowRequest(String username, Integer bookId, Integer numberOfDays) throws Exception {
        Member member = memberRepository.findByAccountUsername(username)
                .orElseThrow(() -> new Exception("Không tìm thấy thông tin độc giả!"));

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new Exception("Sách yêu cầu mượn không tồn tại!"));

        long currentBorrowed = borrowDetailRepository.countActiveBorrowedBooks(member.getMemberId());
        int maxLimit = member.getTier() != null ? member.getTier().getBorrowLimit() : 3;
        if (currentBorrowed >= maxLimit) {
            throw new Exception("Yêu cầu bị từ chối! Bạn đã mượn chạm giới hạn tối đa cho phép (" + maxLimit + " cuốn).");
        }

        Borrow borrow = new Borrow();
        borrow.setMember(member);
        borrow.setBorrowDate(LocalDateTime.now());
        borrow.setStatus("Pending");
        borrow = borrowRepository.save(borrow);

        BorrowDetail detail = new BorrowDetail();
        detail.setBorrow(borrow);
        detail.setBook(book);
        detail.setBookItem(null);
        detail.setDueDate(LocalDateTime.now().plusDays(numberOfDays != null ? numberOfDays : 14));
        detail.setStatus("Pending");
        detail.setRenewCount(0);
        borrowDetailRepository.save(detail);

        return borrow;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processReturnBook(String barcode) throws Exception {
        BookItem item = bookItemRepository.findByBarcode(barcode)
                .orElseThrow(() -> new Exception("Mã vạch sách vật lý không tồn tại!"));

        if (!"Borrowed".equalsIgnoreCase(item.getStatus())) {
            throw new Exception("Sách này hiện tại đang nằm ở trong kho, không có lịch sử cho mượn!");
        }

        BorrowDetail activeDetail = borrowDetailRepository.findAll().stream()
                .filter(d -> d.getBookItem() != null && d.getBookItem().getBookItemId().equals(item.getBookItemId())
                        && ("Borrowed".equalsIgnoreCase(d.getStatus()) || "Overdue".equalsIgnoreCase(d.getStatus())))
                .findFirst()
                .orElseThrow(() -> new Exception("Không tìm thấy lịch sử mượn hợp lệ ứng với mã vạch sách này!"));

        item.setStatus("Available");
        bookItemRepository.save(item);

        activeDetail.setReturnDate(LocalDateTime.now());
        activeDetail.setStatus("Returned");
        borrowDetailRepository.save(activeDetail);

        List<BorrowDetail> sideDetails = borrowDetailRepository.findAll().stream()
                .filter(d -> d.getBorrow() != null && d.getBorrow().getBorrowId().equals(activeDetail.getBorrow().getBorrowId()))
                .collect(Collectors.toList());

        boolean allReturned = sideDetails.stream().allMatch(d -> "Returned".equalsIgnoreCase(d.getStatus()));
        if (allReturned) {
            Borrow parent = activeDetail.getBorrow();
            parent.setStatus("Returned");
            borrowRepository.save(parent);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Borrow> getBorrowsByMemberAndStatus(String username, String status) {
        Integer targetMemberId = accountRepository.findByUsername(username)
                .flatMap(acc -> memberRepository.findByUserId(acc.getUser().getId()))
                .map(Member::getMemberId)
                .orElse(null);

        if (targetMemberId == null) return new ArrayList<>();

        return borrowRepository.findAll().stream()
                .filter(b -> b.getMember() != null
                        && targetMemberId.equals(b.getMember().getMemberId())
                        && status.equalsIgnoreCase(b.getStatus()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Borrow> getAllPendingRequests() {
        return borrowRepository.findAll().stream()
                .filter(b -> "Pending".equalsIgnoreCase(b.getStatus()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Borrow> getAllReturnRequests() {
        return borrowRepository.findAll().stream()
                .filter(b -> "Return_Pending".equalsIgnoreCase(b.getStatus()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Borrow> getAllActiveLoans() {
        return borrowRepository.findAll().stream()
                .filter(b -> "Active".equalsIgnoreCase(b.getStatus()) || "Borrowing".equalsIgnoreCase(b.getStatus()))
                .collect(Collectors.toList());
    }

    // ĐÃ FIX: Chữ ký hàm nhận 2 đối số trùng khớp với yêu cầu từ LibrarianBorrowController
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approvePendingRequest(Integer borrowId, String staffUsername) throws Exception {
        Borrow borrow = borrowRepository.findById(borrowId)
                .orElseThrow(() -> new Exception("Không tìm thấy đơn yêu cầu!"));

        borrow.setStatus("Active");
        borrowRepository.save(borrow);

        List<BorrowDetail> details = borrowDetailRepository.findAll().stream()
                .filter(d -> d.getBorrow() != null && d.getBorrow().getBorrowId().equals(borrowId))
                .collect(Collectors.toList());

        for (BorrowDetail detail : details) {
            detail.setStatus("Borrowed");
            borrowDetailRepository.save(detail);

            // Cập nhật trạng thái sách vật lý sang "Borrowed" nếu có liên kết BookItem
            if (detail.getBookItem() != null) {
                BookItem item = detail.getBookItem();
                item.setStatus("Borrowed");
                bookItemRepository.save(item);
            }
        }

        // UC-8.2: Thủ thư duyệt xong thì trừ phí mượn ngay
        if (borrow.getMember() == null || borrow.getMember().getMemberId() == null) {
            throw new RuntimeException("Phiếu mượn không có member hợp lệ để tính phí mượn.");
        }

        financialService.payBorrowingFee(borrow.getMember().getMemberId(), borrowId);
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveReturnRequest(Integer borrowId) throws Exception {
        Borrow borrow = borrowRepository.findById(borrowId)
                .orElseThrow(() -> new Exception("Không tìm thấy đơn yêu cầu trả!"));
        borrow.setStatus("Returned");
        borrowRepository.save(borrow);

        List<BorrowDetail> details = borrowDetailRepository.findAll().stream()
                .filter(d -> d.getBorrow() != null && d.getBorrow().getBorrowId().equals(borrowId))
                .collect(Collectors.toList());
        for (BorrowDetail detail : details) {
            detail.setStatus("Returned");
            borrowDetailRepository.save(detail);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Integer loanId, String status) throws Exception {
        Borrow borrow = borrowRepository.findById(loanId)
                .orElseThrow(() -> new Exception("Không tìm thấy đơn mượn/trả tương ứng!"));
        borrow.setStatus(status);
        borrowRepository.save(borrow);
    }

    @Override
    @Transactional(readOnly = true)
    public Borrow getBorrowById(Integer borrowId) throws Exception {
        return borrowRepository.findById(borrowId)
                .orElseThrow(() -> new Exception("Không tìm thấy đơn mượn!"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BorrowDetail> getBorrowDetailsByBorrowId(Integer borrowId) {
        return borrowDetailRepository.findAll().stream()
                .filter(d -> d.getBorrow() != null && d.getBorrow().getBorrowId().equals(borrowId))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Borrow> getAllBorrowHistoryByMember(String username) {
        Integer targetMemberId = accountRepository.findByUsername(username)
                .flatMap(acc -> memberRepository.findByUserId(acc.getUser().getId()))
                .map(Member::getMemberId)
                .orElse(null);

        if (targetMemberId == null) return new ArrayList<>();

        return borrowRepository.findAll().stream()
                .filter(b -> b.getMember() != null && targetMemberId.equals(b.getMember().getMemberId()))
                .collect(Collectors.toList());
    }
}