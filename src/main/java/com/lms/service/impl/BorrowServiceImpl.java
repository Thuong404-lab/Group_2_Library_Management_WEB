package com.lms.service.impl;

import com.lms.dto.request.BorrowRequest;
import com.lms.entity.*;
import com.lms.repository.*;
import com.lms.service.BorrowService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class BorrowServiceImpl implements BorrowService {

    private final MemberRepository memberRepository;
    private final BookItemRepository bookItemRepository;
    private final BorrowRepository borrowRepository;
    private final BorrowDetailRepository borrowDetailRepository;
    private final BookRepository bookRepository;
    private final AccountRepository accountRepository;

    public BorrowServiceImpl(MemberRepository memberRepository,
                             BookItemRepository bookItemRepository,
                             BorrowRepository borrowRepository,
                             BorrowDetailRepository borrowDetailRepository,
                             BookRepository bookRepository,
                             AccountRepository accountRepository) {
        this.memberRepository = memberRepository;
        this.bookItemRepository = bookItemRepository;
        this.borrowRepository = borrowRepository;
        this.borrowDetailRepository = borrowDetailRepository;
        this.bookRepository = bookRepository;
        this.accountRepository = accountRepository;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Borrow processBorrowing(BorrowRequest request, String librarianUsername) throws Exception {
        Member member = memberRepository.findByUserEmail(request.getMemberEmail())
                .orElseThrow(() -> new Exception("Member with this email was not found!"));

        if (!"Active".equalsIgnoreCase(member.getUser().getStatus().name())) {
            throw new Exception("This member account is currently locked or inactive!");
        }

        List<BookItem> bookItemsToBorrow = new ArrayList<>();
        for (String barcode : request.getBarcodes()) {
            BookItem item = bookItemRepository.findByBarcode(barcode)
                    .orElseThrow(() -> new Exception("Barcode " + barcode + " does not exist in the system!"));

            if (!"Available".equalsIgnoreCase(item.getStatus())) {
                throw new Exception("The book with barcode " + barcode + " is currently unavailable!");
            }
            bookItemsToBorrow.add(item);
        }

        // TỐI ƯU HÓA: Dùng hàm đếm Query thay vì vòng lặp findAll() làm chậm hệ thống
        long currentBorrowCount = borrowDetailRepository.countActiveBorrowedBooks(member.getMemberId());
        int maxLimit = member.getTier() != null ? member.getTier().getBorrowLimit() : 3;
        int totalRequestedBooks = (int) currentBorrowCount + bookItemsToBorrow.size();

        if (totalRequestedBooks > maxLimit) {
            throw new Exception("The number of requested books exceeds the member tier limit!");
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

    // BỔ SUNG: Xử lý Độc giả gửi form yêu cầu mượn trực tuyến (Trạng thái Pending)
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Borrow memberSubmitBorrowRequest(String username, Integer bookId, Integer numberOfDays) throws Exception {
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new Exception("Không tìm thấy tài khoản người dùng!"));

        Member member = memberRepository.findByUserId(account.getUser().getId())
                .orElseThrow(() -> new Exception("Không tìm thấy thông tin độc giả!"));

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new Exception("Sách yêu cầu mượn không tồn tại!"));

        // Kiểm tra giới hạn mượn theo hạng thành viên
        long currentBorrowed = borrowDetailRepository.countActiveBorrowedBooks(member.getMemberId());
        int maxLimit = member.getTier() != null ? member.getTier().getBorrowLimit() : 3;
        if (currentBorrowed >= maxLimit) {
            throw new Exception("Yêu cầu bị từ chối! Bạn đã mượn chạm giới hạn tối đa cho phép (" + maxLimit + " cuốn).");
        }

        // Tạo đơn mượn tổng với trạng thái PENDING
        Borrow borrow = new Borrow();
        borrow.setMember(member);
        borrow.setBorrowDate(LocalDateTime.now());
        borrow.setStatus("Pending");
        borrow = borrowRepository.save(borrow);

        // Tạo chi tiết yêu cầu
        BorrowDetail detail = new BorrowDetail();
        detail.setBorrow(borrow);
        detail.setBook(book);
        detail.setBookItem(null); // Trống: Thủ thư sẽ chỉ định bản sao vật lý cụ thể tại kho khi duyệt đơn
        detail.setDueDate(LocalDateTime.now().plusDays(numberOfDays != null ? numberOfDays : 14));
        detail.setStatus("Pending");
        detail.setRenewCount(0);
        borrowDetailRepository.save(detail);

        return borrow;
    }

    // BỔ SUNG: Nghiệp vụ xử lý Trả sách và cập nhật trạng thái kho
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processReturnBook(String barcode) throws Exception {
        BookItem item = bookItemRepository.findByBarcode(barcode)
                .orElseThrow(() -> new Exception("Mã vạch sách vật lý không tồn tại!"));

        if (!"Borrowed".equalsIgnoreCase(item.getStatus())) {
            throw new Exception("Sách này hiện tại đang nằm ở trong kho, không có lịch sử cho mượn!");
        }

        // Tìm kiếm bản ghi mượn chưa trả chứa cuốn sách này
        BorrowDetail activeDetail = borrowDetailRepository.findAll().stream()
                .filter(d -> d.getBookItem() != null && d.getBookItem().getBookItemId().equals(item.getBookItemId())
                        && ("Borrowed".equalsIgnoreCase(d.getStatus()) || "Overdue".equalsIgnoreCase(d.getStatus())))
                .findFirst()
                .orElseThrow(() -> new Exception("Không tìm thấy lịch sử mượn hợp lệ ứng với mã vạch sách này!"));

        // Giải phóng trạng thái sách trong kho vật lý
        item.setStatus("Available");
        bookItemRepository.save(item);

        // Đóng trạng thái chi tiết mượn
        activeDetail.setReturnDate(LocalDateTime.now());
        activeDetail.setStatus("Returned");
        borrowDetailRepository.save(activeDetail);

        // Nếu tất cả các cuốn sách trong cùng một đơn mượn tổng đã hoàn trả, chuyển trạng thái đơn tổng thành Returned
        List<BorrowDetail> sideDetails = borrowDetailRepository.findByBorrowId(activeDetail.getBorrow().getBorrowId());
        boolean allReturned = sideDetails.stream().allMatch(d -> "Returned".equalsIgnoreCase(d.getStatus()));
        if (allReturned) {
            Borrow parent = activeDetail.getBorrow();
            parent.setStatus("Returned");
            borrowRepository.save(parent);
        }
    }
}