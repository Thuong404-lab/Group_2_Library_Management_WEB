package com.lms.service.impl;

import com.lms.dto.request.BorrowRequest;
import com.lms.dto.response.MemberBorrowDTO;
import com.lms.dto.response.ReservationRequestDTO;
import com.lms.dto.response.ReturnRequestDTO;
import com.lms.entity.*;
import com.lms.enums.ActionType;
import com.lms.enums.UserStatus;
import com.lms.repository.*;
import com.lms.service.AuditLogService;
import com.lms.service.BorrowService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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
    private final MemberAccountRepository memberAccountRepository;
    private final SystemSettingRepository systemSettingRepository;
    private final ReservationRepository reservationRepository;
    private final AuditLogService auditLogService;
    private final NotificationRepository notificationRepository;
    private final MemberNotificationRepository memberNotificationRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    public BorrowServiceImpl(MemberRepository memberRepository,
                             BookItemRepository bookItemRepository,
                             BorrowRepository borrowRepository,
                             BorrowDetailRepository borrowDetailRepository,
                             BookRepository bookRepository,
                             MemberAccountRepository memberAccountRepository,
                             SystemSettingRepository systemSettingRepository,
                             ReservationRepository reservationRepository,
                             AuditLogService auditLogService,
                             NotificationRepository notificationRepository,
                             MemberNotificationRepository memberNotificationRepository,
                             WalletRepository walletRepository,
                             TransactionRepository transactionRepository) { // Thêm vào tham số nhận
        this.memberRepository = memberRepository;
        this.bookItemRepository = bookItemRepository;
        this.borrowRepository = borrowRepository;
        this.borrowDetailRepository = borrowDetailRepository;
        this.bookRepository = bookRepository;
        this.memberAccountRepository = memberAccountRepository;
        this.systemSettingRepository = systemSettingRepository;
        this.reservationRepository = reservationRepository;
        this.auditLogService = auditLogService;
        this.notificationRepository = notificationRepository;
        this.memberNotificationRepository = memberNotificationRepository;
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Borrow processBorrowing(BorrowRequest request, String librarianUsername) {
        String identifier = request.getMemberIdentifier() != null && !request.getMemberIdentifier().isBlank()
                ? request.getMemberIdentifier().trim()
                : request.getMemberEmail();
        Member member = memberRepository.findByUserEmail(identifier)
                .or(() -> memberRepository.findByUserPhone(identifier))
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay doc gia voi email hoac so dien thoai nay!"));

        if (member.getUser() != null && member.getUser().getStatus() != UserStatus.Active) {
            throw new IllegalArgumentException("Tai khoan thanh vien nay dang bi khoa hoac chua kich hoat!");
        }

        List<BookItem> bookItemsToBorrow = new ArrayList<>();
        for (String barcode : request.getBarcodes()) {
            BookItem item = bookItemRepository.findByBarcode(barcode)
                    .orElseThrow(() -> new IllegalArgumentException("Ma vach " + barcode + " khong ton tai!"));
            if (!"Available".equalsIgnoreCase(item.getStatus())) {
                throw new IllegalArgumentException("Sach co ma vach " + barcode + " hien tai khong san sang!");
            }
            bookItemsToBorrow.add(item);
        }

        long currentBorrowCount = borrowDetailRepository.countActiveBorrowedBooks(member.getMemberId());
        int maxLimit = getEffectiveBorrowLimit(member);
        if (currentBorrowCount + bookItemsToBorrow.size() > maxLimit) {
            throw new IllegalArgumentException("So luong sach vuot qua gioi han muon cua hang thanh vien!");
        }

        int borrowDays = normalizeBorrowDays(request.getNumberOfDays());
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
            detail.setDueDate(LocalDateTime.now().plusDays(borrowDays));
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
    public Borrow memberSubmitBorrowRequest(String username, Integer bookId, Integer numberOfDays) {
        Member member = memberRepository.findByAccountUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay thong tin doc gia!"));
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Sach yeu cau muon khong ton tai!"));

        long currentBorrowed = borrowDetailRepository.countActiveBorrowedBooks(member.getMemberId());
        int maxLimit = getEffectiveBorrowLimit(member);
        if (currentBorrowed >= maxLimit) {
            throw new IllegalArgumentException("Yeu cau bi tu choi! Ban da muon cham gioi han toi da cho phep (" + maxLimit + " cuon).");
        }

        int borrowDays = normalizeBorrowDays(numberOfDays);
        Borrow borrow = new Borrow();
        borrow.setMember(member);
        borrow.setBorrowDate(LocalDateTime.now());
        borrow.setStatus("Pending");
        borrow = borrowRepository.save(borrow);

        BorrowDetail detail = new BorrowDetail();
        detail.setBorrow(borrow);
        detail.setBook(book);
        
        List<BookItem> items = bookItemRepository.findByBook_BookId(book.getBookId());
        BookItem availableItem = items.stream()
                .filter(item -> "Available".equalsIgnoreCase(item.getStatus()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Đầu sách này hiện tại không còn bản sách vật lý nào sẵn sàng!"));
        detail.setBookItem(availableItem);
        availableItem.setStatus("Pending");
        bookItemRepository.save(availableItem);

        detail.setDueDate(LocalDateTime.now().plusDays(borrowDays));
        detail.setStatus("Pending");
        detail.setRenewCount(0);
        borrowDetailRepository.save(detail);

        auditLogService.log(
                ActionType.REQUEST_BORROW,
                "Member " + username + " gui yeu cau muon sach #" + book.getBookId()
                        + " - " + book.getTitle() + " trong " + borrowDays + " ngay.");
        return borrow;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approvePendingRequest(Integer borrowId, String staffUsername) {
        Borrow borrow = borrowRepository.findById(borrowId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay don yeu cau muon!"));
        borrow.setStatus("Active");
        borrowRepository.save(borrow);

        List<BorrowDetail> details = getBorrowDetailsByBorrowId(borrowId);
        for (BorrowDetail detail : details) {
            detail.setStatus("Borrowed");
            if (detail.getBookItem() == null) {
                List<BookItem> items = bookItemRepository.findByBook_BookId(detail.getBook().getBookId());
                BookItem availableItem = items.stream()
                        .filter(item -> "Available".equalsIgnoreCase(item.getStatus()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Không còn bản sách vật lý nào sẵn sàng cho cuốn sách: " + detail.getBook().getTitle()));
                detail.setBookItem(availableItem);
                availableItem.setStatus("Borrowed");
                bookItemRepository.save(availableItem);
            } else {
                BookItem item = detail.getBookItem();
                item.setStatus("Borrowed");
                bookItemRepository.save(item);
            }
            borrowDetailRepository.save(detail);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void memberSubmitReturnRequest(String username, Integer borrowDetailId) {
        Integer memberId = getMemberIdByUsername(username);
        BorrowDetail detail = borrowDetailRepository.findById(borrowDetailId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay chi tiet phieu muon tuong ung!"));
        if (memberId == null || detail.getBorrow() == null || detail.getBorrow().getMember() == null
                || !memberId.equals(detail.getBorrow().getMember().getMemberId())) {
            throw new IllegalArgumentException("Ban khong co quyen gui yeu cau tra sach nay!");
        }
        if (!"Borrowed".equalsIgnoreCase(detail.getStatus()) && !"Overdue".equalsIgnoreCase(detail.getStatus())) {
            throw new IllegalArgumentException("Trang thai sach hien tai khong hop le de gui yeu cau tra!");
        }

        detail.setStatus("Return_Pending");
        borrowDetailRepository.save(detail);

        Borrow parent = detail.getBorrow();
        parent.setStatus("Return_Pending");
        borrowRepository.save(parent);

        auditLogService.log(
                ActionType.REQUEST_RETURN,
                "Member " + username + " gui yeu cau tra sach #" + parent.getBorrowId() + ".");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void memberSubmitRenewRequest(Integer borrowDetailId) {
        BorrowDetail detail = borrowDetailRepository.findById(borrowDetailId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay chi tiet phieu muon!"));

        if (!"Borrowed".equalsIgnoreCase(detail.getStatus())) {
            throw new IllegalArgumentException("Chi sach dang o trang thai 'Dang muon' moi duoc phep yeu cau gia han!");
        }

        int maxRenewals = getPositiveIntSetting("MAX_RENEWALS", 2);
        if (detail.getRenewCount() != null && detail.getRenewCount() >= maxRenewals) {
            throw new IllegalArgumentException("Sach nay da duoc gia han toi da " + maxRenewals + " lan!");
        }

        detail.setStatus("Renew_Pending");
        borrowDetailRepository.save(detail);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveReturnRequest(Integer borrowId) {
        Borrow borrow = borrowRepository.findById(borrowId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay don yeu cau tra!"));
        borrow.setStatus("Returned");
        borrowRepository.save(borrow);

        for (BorrowDetail detail : getBorrowDetailsByBorrowId(borrowId)) {
            detail.setStatus("Returned");
            detail.setReturnDate(LocalDateTime.now());
            borrowDetailRepository.save(detail);
            if (detail.getBookItem() != null) {
                BookItem item = detail.getBookItem();
                item.setStatus("Available");
                bookItemRepository.save(item);
            }
        }

        sendInternalNotification(borrow.getMember(), "Xac nhan tra sach thanh cong",
                "Yeu cau tra sach cua phieu muon #" + borrowId + " da duoc thu thu phe duyet.");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processReturnBook(String barcode) {
        BookItem item = bookItemRepository.findByBarcode(barcode)
                .orElseThrow(() -> new IllegalArgumentException("Ma vach sach vat ly khong ton tai!"));
        BorrowDetail activeDetail = borrowDetailRepository.findAll().stream()
                .filter(d -> d.getBookItem() != null && d.getBookItem().getBookItemId().equals(item.getBookItemId())
                        && ("Borrowed".equalsIgnoreCase(d.getStatus())
                        || "Overdue".equalsIgnoreCase(d.getStatus())
                        || "Return_Pending".equalsIgnoreCase(d.getStatus())))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay lich su muon hop le ung voi ma vach sach nay!"));

        item.setStatus("Available");
        bookItemRepository.save(item);
        activeDetail.setReturnDate(LocalDateTime.now());
        activeDetail.setStatus("Returned");
        borrowDetailRepository.save(activeDetail);

        Borrow parent = activeDetail.getBorrow();
        List<BorrowDetail> sideDetails = getBorrowDetailsByBorrowId(parent.getBorrowId());
        if (sideDetails.stream().allMatch(d -> "Returned".equalsIgnoreCase(d.getStatus()))) {
            parent.setStatus("Returned");
            borrowRepository.save(parent);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Reservation memberSubmitReservationRequest(String username, Integer bookId) {
        Member member = memberRepository.findByAccountUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Tai khoan doc gia khong ton tai!"));
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Sach yeu cau dat truoc khong ton tai!"));

        boolean alreadyReserved = reservationRepository.findByMember_MemberIdOrderByReservationDateDesc(member.getMemberId())
                .stream()
                .anyMatch(r -> r.getBook() != null
                        && r.getBook().getBookId().equals(bookId)
                        && ("Pending".equalsIgnoreCase(r.getStatus()) || "Active".equalsIgnoreCase(r.getStatus())));
        if (alreadyReserved) {
            throw new IllegalArgumentException("Ban da co mot yeu cau dat truoc cuon sach nay va dang cho xu ly!");
        }

        Reservation reservation = new Reservation();
        reservation.setMember(member);
        reservation.setBook(book);
        reservation.setReservationDate(LocalDateTime.now());
        reservation.setStatus("Pending");
        Reservation savedReservation = reservationRepository.save(reservation);
        auditLogService.log(
                ActionType.RESERVE_BOOK,
                "Member " + username + " gui yeu cau dat truoc sach #" + book.getBookId()
                        + " - " + book.getTitle() + ".");
        return savedReservation;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveReservationRequest(Integer reservationId, String staffUsername) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay don yeu cau dat truoc!"));
        if (!"Pending".equalsIgnoreCase(reservation.getStatus())) {
            throw new IllegalArgumentException("Don dat truoc nay da duoc xu ly tu truoc!");
        }

        reservation.setStatus("Active");
        reservationRepository.save(reservation);
        sendInternalNotification(reservation.getMember(), "Yeu cau dat truoc duoc phe duyet",
                "Cuon sach '" + reservation.getBook().getTitle() + "' da duoc dat giu thanh cong.");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void memberCancelReservation(String username, Integer reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Don dat truoc khong ton tai!"));
        Integer currentMemberId = getMemberIdByUsername(username);
        if (currentMemberId == null || !reservation.getMember().getMemberId().equals(currentMemberId)) {
            throw new IllegalArgumentException("Ban khong co quyen huy don dat truoc cua nguoi khac!");
        }
        reservation.setStatus("Canceled");
        reservationRepository.save(reservation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReturnRequestDTO> getPendingReturnRequestDTOs() {
        return borrowDetailRepository.findByStatus("Return_Pending").stream()
                .map(bd -> {
                    Member member = bd.getBorrow().getMember();
                    String name = member != null && member.getUser() != null ? member.getUser().getFullName() : "N/A";
                    String email = member != null && member.getUser() != null ? member.getUser().getEmail() : "N/A";
                    return new ReturnRequestDTO(
                            bd.getBorrowDetailId(),
                            name,
                            email,
                            bd.getBook().getTitle(),
                            bd.getBookItem() != null ? bd.getBookItem().getBarcode() : "N/A",
                            bd.getBorrow().getBorrowDate());
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Reservation> getAllPendingReservations() {
        return reservationRepository.findAll().stream()
                .filter(r -> "Pending".equalsIgnoreCase(r.getStatus()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservationRequestDTO> getPendingReservationDTOs() {
        return getAllPendingReservations().stream()
                .map(r -> new ReservationRequestDTO(
                        r.getReservationId(),
                        r.getMember() != null && r.getMember().getUser() != null ? r.getMember().getUser().getFullName() : "N/A",
                        r.getBook().getTitle(),
                        r.getReservationDate(),
                        1))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Borrow> getBorrowsByMemberAndStatus(String username, String status) {
        Integer id = getMemberIdByUsername(username);
        return id == null ? new ArrayList<>() : borrowRepository.findAll().stream()
                .filter(b -> b.getMember() != null && id.equals(b.getMember().getMemberId()) && status.equalsIgnoreCase(b.getStatus()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Borrow> getAllPendingRequests() {
        return borrowRepository.findAll().stream()
                .filter(b -> "Pending".equalsIgnoreCase(b.getStatus()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Borrow> getAllReturnRequests() {
        return borrowRepository.findAll().stream()
                .filter(b -> "Return_Pending".equalsIgnoreCase(b.getStatus()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Borrow> getAllActiveLoans() {
        return borrowRepository.findAll().stream()
                .filter(b -> "Active".equalsIgnoreCase(b.getStatus()) || "Borrowing".equalsIgnoreCase(b.getStatus()))
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Integer loanId, String status) {
        Borrow borrow = borrowRepository.findById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn mượn/tra tương ứng!"));

        String oldStatus = borrow.getStatus();
        borrow.setStatus(status);
        borrowRepository.save(borrow);

        List<BorrowDetail> details = getBorrowDetailsByBorrowId(loanId);
        for (BorrowDetail detail : details) {
            detail.setStatus(status);
            borrowDetailRepository.save(detail);
            if (detail.getBookItem() != null && "Rejected".equalsIgnoreCase(status)) {
                BookItem item = detail.getBookItem();
                item.setStatus("Available");
                bookItemRepository.save(item);
            }
        }

        // KÍCH HOẠT HOÀN TIỀN: Nếu chuyển trạng thái từ Pending sang Rejected
        if ("Pending".equalsIgnoreCase(oldStatus) && "Rejected".equalsIgnoreCase(status)) {
            // Tìm giao dịch trừ tiền phí mượn ban đầu tương ứng với phiếu mượn này
            List<Transaction> feeTxns = transactionRepository.findAll().stream()
                    .filter(t -> t.getBorrow() != null && t.getBorrow().getBorrowId().equals(loanId) && "BORROW_FEE".equals(t.getTransactionType()))
                    .toList();

            for (Transaction originalTxn : feeTxns) {
                Wallet wallet = originalTxn.getWallet();
                BigDecimal refundAmount = originalTxn.getAmount();

                // Trả tiền lại vào ví độc giả
                wallet.setBalance(wallet.getBalance().add(refundAmount));
                walletRepository.save(wallet);

                // Ghi nhận giao dịch hoàn tiền mới
                Transaction refundTxn = new Transaction();
                refundTxn.setWallet(wallet);
                refundTxn.setBorrow(borrow);
                refundTxn.setTransactionType("REFUND_FEE");
                refundTxn.setAmount(refundAmount);
                refundTxn.setTransactionDate(LocalDateTime.now());
                refundTxn.setStatus("Completed");
                transactionRepository.save(refundTxn);

                // Gửi thông báo hoàn phí
                sendInternalNotification(borrow.getMember(), "Hoàn phí mượn sách thành công",
                        "Yêu cầu mượn sách #" + loanId + " bị từ chối. Thư viện đã hoàn trả lại " + refundAmount + " VND vào ví điện tử của bạn.");
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Borrow getBorrowById(Integer borrowId) {
        return borrowRepository.findById(borrowId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay don muon!"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BorrowDetail> getBorrowDetailsByBorrowId(Integer borrowId) {
        return borrowDetailRepository.findAll().stream()
                .filter(d -> d.getBorrow() != null && d.getBorrow().getBorrowId().equals(borrowId))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Borrow> getAllBorrowHistoryByMember(String username) {
        Integer id = getMemberIdByUsername(username);
        return id == null ? new ArrayList<>() : borrowRepository.findAll().stream()
                .filter(b -> b.getMember() != null && id.equals(b.getMember().getMemberId()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemberBorrowDTO> getMemberCurrentBorrows(String username) {
        Integer memberId = getMemberIdByUsername(username);
        if (memberId == null) return new ArrayList<>();
        return borrowDetailRepository.findCurrentBorrowsByMemberId(memberId).stream()
                .filter(d -> "Pending".equalsIgnoreCase(d.getStatus())
                        || "Borrowed".equalsIgnoreCase(d.getStatus())
                        || "Overdue".equalsIgnoreCase(d.getStatus())
                        || "Return_Pending".equalsIgnoreCase(d.getStatus())
                        || "Renew_Pending".equalsIgnoreCase(d.getStatus()))
                .map(this::toMemberBorrowDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemberBorrowDTO> getMemberReservations(String username) {
        Integer memberId = getMemberIdByUsername(username);
        if (memberId == null) return new ArrayList<>();

        List<MemberBorrowDTO> dtoList = new ArrayList<>();
        for (Reservation res : reservationRepository.findByMember_MemberIdOrderByReservationDateDesc(memberId)) {
            if (!"Pending".equalsIgnoreCase(res.getStatus()) && !"Active".equalsIgnoreCase(res.getStatus())) {
                continue;
            }
            MemberBorrowDTO dto = new MemberBorrowDTO();
            dto.setId(res.getReservationId());
            dto.setBookTitle(res.getBook().getTitle());
            dto.setAuthorName(getAuthorNames(res.getBook()));
            dto.setBookIdStr("RES-" + res.getBook().getBookId());
            dto.setActionDate(res.getReservationDate());
            if (res.getReservationDate() != null) {
                dto.setDueDate(res.getReservationDate().plusDays(3));
            }
            dto.setStatus(res.getStatus());
            dtoList.add(dto);
        }
        return dtoList;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemberBorrowDTO> getMemberOneMonthHistory(String username) {
        Integer memberId = getMemberIdByUsername(username);
        if (memberId == null) return new ArrayList<>();
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);
        return borrowDetailRepository.findBorrowHistoryInOneMonth(memberId, oneMonthAgo).stream()
                .filter(d -> "Returned".equalsIgnoreCase(d.getStatus()))
                .map(this::toMemberBorrowDTO)
                .toList();
    }

    private MemberBorrowDTO toMemberBorrowDTO(BorrowDetail detail) {
        MemberBorrowDTO dto = new MemberBorrowDTO();
        dto.setId(detail.getBorrowDetailId());
        dto.setBookTitle(detail.getBook().getTitle());
        dto.setAuthorName(getAuthorNames(detail.getBook()));
        dto.setBookIdStr(detail.getBookItem() != null ? detail.getBookItem().getBarcode() : "BK-" + detail.getBook().getBookId());
        dto.setActionDate(detail.getBorrow().getBorrowDate());
        dto.setDueDate(detail.getDueDate());
        dto.setReturnDate(detail.getReturnDate());
        dto.setStatus(detail.getStatus());
        if (detail.getBorrow().getBorrowDate() != null && detail.getDueDate() != null) {
            long totalDays = ChronoUnit.DAYS.between(detail.getBorrow().getBorrowDate(), detail.getDueDate());
            long elapsedDays = ChronoUnit.DAYS.between(detail.getBorrow().getBorrowDate(), LocalDateTime.now());
            long daysLeft = ChronoUnit.DAYS.between(LocalDateTime.now(), detail.getDueDate());
            dto.setDaysLeft(Math.max(0, daysLeft));
            int progress = totalDays > 0 ? (int) ((elapsedDays * 100) / totalDays) : 0;
            dto.setProgressPercentage(Math.min(100, Math.max(0, progress)));
        }
        dto.setRenewCount(detail.getRenewCount() != null ? detail.getRenewCount() : 0);
        return dto;
    }

    private Integer getMemberIdByUsername(String username) {
        return memberAccountRepository.findByUsername(username)
                .map(MemberAccount::getMember)
                .map(Member::getMemberId)
                .orElse(null);
    }

    private void sendInternalNotification(Member member, String title, String content) {
        if (member == null || member.getMemberId() == null) {
            return;
        }
        Notification notif = new Notification();
        notif.setTitle(title);
        notif.setContent(content);
        notif.setCreatedDate(LocalDateTime.now());
        notif.setStatus("Active");
        Notification saved = notificationRepository.save(notif);

        MemberNotification mn = new MemberNotification();
        mn.setId(new MemberNotificationId(member.getMemberId(), saved.getNotificationId()));
        mn.setMember(member);
        mn.setNotification(saved);
        mn.setIsRead(false);
        memberNotificationRepository.save(mn);
    }

    private String getAuthorNames(Book book) {
        if (book.getAuthors() == null || book.getAuthors().isEmpty()) {
            return "Chua ro tac gia";
        }
        return book.getAuthors().stream()
                .map(Author::getAuthorName)
                .collect(Collectors.joining(", "));
    }

    private int getEffectiveBorrowLimit(Member member) {
        int configuredLimit = getPositiveIntSetting("MAX_BOOKS_PER_MEMBER", 10);
        int tierLimit = member != null && member.getTier() != null && member.getTier().getBorrowLimit() != null
                ? member.getTier().getBorrowLimit()
                : configuredLimit;
        return Math.max(1, Math.min(configuredLimit, tierLimit));
    }

    private int normalizeBorrowDays(Integer requestedDays) {
        int maxBorrowDays = getPositiveIntSetting("MAX_BORROW_DAYS", 14);
        int safeRequestedDays = requestedDays == null || requestedDays <= 0 ? maxBorrowDays : requestedDays;
        return Math.min(safeRequestedDays, maxBorrowDays);
    }

    private int getPositiveIntSetting(String key, int defaultValue) {
        try {
            return systemSettingRepository.findBySettingKeyIgnoreCase(key)
                    .map(SystemSetting::getSettingValue)
                    .filter(value -> value != null && !value.isBlank())
                    .map(String::trim)
                    .map(Integer::parseInt)
                    .filter(value -> value > 0)
                    .orElse(defaultValue);
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    @org.springframework.context.event.EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
    @Transactional(rollbackFor = Exception.class)
    public void repairExistingData() {
        try {
            List<BorrowDetail> nullItemDetails = borrowDetailRepository.findAll().stream()
                    .filter(d -> d.getBookItem() == null && 
                            ("Borrowed".equalsIgnoreCase(d.getStatus()) 
                             || "Overdue".equalsIgnoreCase(d.getStatus()) 
                             || "Return_Pending".equalsIgnoreCase(d.getStatus())
                             || "Pending".equalsIgnoreCase(d.getStatus())))
                    .toList();
            for (BorrowDetail detail : nullItemDetails) {
                List<BookItem> items = bookItemRepository.findByBook_BookId(detail.getBook().getBookId());
                BookItem availableItem = items.stream()
                        .filter(item -> "Available".equalsIgnoreCase(item.getStatus()))
                        .findFirst()
                        .orElse(null);
                if (availableItem != null) {
                    detail.setBookItem(availableItem);
                    if ("Pending".equalsIgnoreCase(detail.getStatus())) {
                        availableItem.setStatus("Pending");
                    } else {
                        availableItem.setStatus("Borrowed");
                    }
                    bookItemRepository.save(availableItem);
                    borrowDetailRepository.save(detail);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to repair existing data: " + e.getMessage());
        }
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectReservationRequest(Integer reservationId, String staffUsername) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay don dat truoc de tu choi!"));
        if (!"Pending".equalsIgnoreCase(reservation.getStatus())) {
            throw new IllegalArgumentException("Don dat truoc nay da duoc xu ly, khong the tu choi!");
        }
        reservation.setStatus("Rejected");
        reservationRepository.save(reservation);

        sendInternalNotification(reservation.getMember(), "Yeu cau dat truoc bi tu choi",
                "Yeu cau dat truoc cuon sach '" + reservation.getBook().getTitle() + "' da bi tu choi.");
    }

    @Override
    @Transactional(readOnly = true)
    public Reservation getReservationById(Integer reservationId) {
        return reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay don dat truoc voi ID: " + reservationId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BorrowDetail> getPendingRenewalRequests() {
        // Lay tat ca cac chi tiet phieu muon dang o trang thai cho gia han
        return borrowDetailRepository.findAll().stream()
                .filter(d -> "Renew_Pending".equalsIgnoreCase(d.getStatus()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BorrowDetail getBorrowDetailById(Integer borrowDetailId) {
        return borrowDetailRepository.findById(borrowDetailId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay chi tiet phieu muon voi ID: " + borrowDetailId));
    }

    @Override
    public int getMaxBorrowDays() {
        // Goi lai ham doc cau hinh he thong san co cua ban
        return getPositiveIntSetting("MAX_BORROW_DAYS", 14);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateBorrowFeePreview(String username, List<Integer> bookIds, Integer numberOfDays) {
        if (bookIds == null || bookIds.isEmpty()) return BigDecimal.ZERO;

        Member member = memberRepository.findByAccountUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin độc giả!"));

        int borrowDays = normalizeBorrowDays(numberOfDays);

        // Đọc cấu hình phí cơ bản mặc định từ hệ thống (Vd: FEE_PER_BOOK_PER_DAY = 5000 VND)
        BigDecimal feePerBookPerDay = BigDecimal.valueOf(getPositiveIntSetting("FEE_PER_BOOK_PER_DAY", 5000));

        // Đọc tỷ lệ chiết khấu từ hạng thành viên (Vd: hạng Vàng giảm 10% thì discountPercent = 10)
        double discountPercent = (member.getTier() != null && member.getTier().getDiscountPercent() != null)
                ? member.getTier().getDiscountPercent().doubleValue() : 0.0;
        BigDecimal discountFactor = BigDecimal.valueOf(1.0 - (discountPercent / 100.0));

        BigDecimal totalFee = feePerBookPerDay
                .multiply(BigDecimal.valueOf(bookIds.size()))
                .multiply(BigDecimal.valueOf(borrowDays))
                .multiply(discountFactor);

        return totalFee;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Borrow memberSubmitMultiBookBorrowRequest(String username, List<Integer> bookIds, Integer numberOfDays) {
        if (bookIds == null || bookIds.isEmpty()) {
            throw new IllegalArgumentException("Giỏ sách trống! Vui lòng chọn ít nhất một cuốn sách.");
        }

        Member member = memberRepository.findByAccountUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin độc giả!"));

        // 1. Kiểm tra giới hạn mượn
        long currentBorrowed = borrowDetailRepository.countActiveBorrowedBooks(member.getMemberId());
        int maxLimit = getEffectiveBorrowLimit(member);
        if (currentBorrowed + bookIds.size() > maxLimit) {
            throw new IllegalArgumentException("Yêu cầu bị từ chối! Số lượng sách đăng ký vượt quá giới hạn mượn còn lại của bạn (Tối đa còn: " + (maxLimit - currentBorrowed) + " cuốn).");
        }

        int borrowDays = normalizeBorrowDays(numberOfDays);

        // 2. Tính toán tổng chi phí trừ ví
        BigDecimal totalFee = calculateBorrowFeePreview(username, bookIds, borrowDays);

        Wallet wallet = walletRepository.findByMemberMemberId(member.getMemberId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản ví điện tử thành viên!"));
        if (wallet.getBalance().compareTo(totalFee) < 0) {
            throw new IllegalArgumentException("Số dư ví không đủ để thanh toán phí mượn sách! Vui lòng nạp thêm tiền.");
        }

        // 3. Khởi tạo phiếu mượn gốc (Borrow)
        Borrow borrow = new Borrow();
        borrow.setMember(member);
        borrow.setBorrowDate(LocalDateTime.now());
        borrow.setStatus("Pending");
        borrow = borrowRepository.save(borrow);

        // 4. Trừ tiền ví & Ghi nhận lịch sử giao dịch tài chính
        wallet.setBalance(wallet.getBalance().subtract(totalFee)); // update balance
        walletRepository.save(wallet);

        Transaction transaction = new Transaction();
        transaction.setWallet(wallet);
        transaction.setBorrow(borrow);
        transaction.setTransactionType("BORROW_FEE");
        transaction.setAmount(totalFee);
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setStatus("Completed");
        transactionRepository.save(transaction);

        // 5. Tìm bản sách vật lý khả dụng ứng với từng đầu sách được chọn và thiết lập BorrowDetail
        List<String> titles = new ArrayList<>();
        for (Integer bookId : bookIds) {
            Book book = bookRepository.findById(bookId)
                    .orElseThrow(() -> new IllegalArgumentException("Sách mang mã số #" + bookId + " không tồn tại!"));

            List<BookItem> items = bookItemRepository.findByBook_BookId(book.getBookId());
            BookItem availableItem = items.stream()
                    .filter(item -> "Available".equalsIgnoreCase(item.getStatus()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Đầu sách '" + book.getTitle() + "' hiện tại đã hết bản vật lý sẵn sàng!"));

            availableItem.setStatus("Pending");
            bookItemRepository.save(availableItem);

            BorrowDetail detail = new BorrowDetail();
            detail.setBorrow(borrow);
            detail.setBook(book);
            detail.setBookItem(availableItem);
            detail.setDueDate(LocalDateTime.now().plusDays(borrowDays));
            detail.setStatus("Pending");
            detail.setRenewCount(0);
            borrowDetailRepository.save(detail);

            titles.add(book.getTitle());
        }

        // 6. Ghi nhật ký hệ thống (Audit Log)
        auditLogService.log(ActionType.REQUEST_BORROW,
                "Độc giả " + username + " đã đăng ký mượn tập trung " + bookIds.size() + " cuốn sách: "
                        + String.join(", ", titles) + " trong vòng " + borrowDays + " ngày. Phí trừ ví: " + totalFee + " VND.");

        return borrow;
    }

}
