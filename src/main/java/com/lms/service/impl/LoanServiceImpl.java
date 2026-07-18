package com.lms.service.impl;

import com.lms.entity.*;
import com.lms.exception.ConflictException;
import com.lms.exception.DataProcessingException;
import com.lms.exception.ResourceNotFoundException;
import com.lms.exception.ValidationException;
import com.lms.repository.*;
import com.lms.service.LoanService;
import com.lms.service.LocalizedMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * LoanServiceImpl - Xử lý Logic Quản lý Phiếu mượn (Thủ thư)
 * Người phụ trách: Huỳnh Gia Hưng (CE190488)
 */
@Service
public class LoanServiceImpl implements LoanService {

    @Autowired
    private LocalizedMessageService localizedMessageService = LocalizedMessageService.fallback();

    private static final String STATUS_BORROWED = "Borrowed";
    private static final String STATUS_AVAILABLE = "Available";
    private static final String STATUS_ACTIVE = "Active";
    private static final String STATUS_RETURNED = "Returned";
    private static final String STATUS_OVERDUE = "Overdue";
    private final BorrowRepository borrowRepository;
    private final BorrowDetailRepository borrowDetailRepository;
    private final MemberRepository memberRepository;
    private final StaffAccountRepository staffAccountRepository;
    private final BookItemRepository bookItemRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final SystemSettingRepository systemSettingRepository;
    private final NotificationRepository notificationRepository;
    private final MemberNotificationRepository memberNotificationRepository;


    public LoanServiceImpl(BorrowRepository borrowRepository,
                           BorrowDetailRepository borrowDetailRepository,
                           MemberRepository memberRepository,
                           StaffAccountRepository staffAccountRepository,
                           BookItemRepository bookItemRepository,
                           WalletRepository walletRepository,
                           TransactionRepository transactionRepository,
                           SystemSettingRepository systemSettingRepository,
                           NotificationRepository notificationRepository,
                           MemberNotificationRepository memberNotificationRepository) {
        this.borrowRepository = borrowRepository;
        this.borrowDetailRepository = borrowDetailRepository;
        this.memberRepository = memberRepository;
        this.staffAccountRepository = staffAccountRepository;
        this.bookItemRepository = bookItemRepository;
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.systemSettingRepository = systemSettingRepository;
        this.notificationRepository = notificationRepository;
        this.memberNotificationRepository = memberNotificationRepository;
    }

    // UC-13.1: Xem chi tiết phiếu mượn
    @Override
    @Transactional(readOnly = true)
    public Borrow getLoanDetails(Integer borrowId) {
        return borrowRepository.findById(borrowId)
                .orElseThrow(() -> new ResourceNotFoundException(localizedMessageService.get("backend.loan.notFoundById", borrowId)));
    }

    // UC-13.2: Xác nhận trả sách trực tiếp bằng quét mã vạch (Barcode) tại quầy
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmReturn(String barcode) {
        BookItem item = bookItemRepository.findByBarcode(barcode)
                .orElseThrow(() -> new ResourceNotFoundException(localizedMessageService.get("backend.loan.barcodeNotFound", barcode)));

        BorrowDetail activeDetail = borrowDetailRepository.findAll().stream()
                .filter(d -> d.getBookItem() != null && d.getBookItem().getBookItemId().equals(item.getBookItemId())
                        && (STATUS_BORROWED.equalsIgnoreCase(d.getStatus()) || STATUS_OVERDUE.equalsIgnoreCase(d.getStatus()) || "Return_Pending".equalsIgnoreCase(d.getStatus())))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(localizedMessageService.get("backend.loan.activeHistoryNotFound")));

        // 1. Cập nhật trạng thái sách vật lý
        item.setStatus(STATUS_AVAILABLE);
        bookItemRepository.save(item);

        // 2. Tính toán phạt quá hạn nếu có
        processOverdueFine(activeDetail);

        // 3. Cập nhật trạng thái chi tiết mượn
        activeDetail.setReturnDate(LocalDateTime.now());
        activeDetail.setStatus(STATUS_RETURNED);
        borrowDetailRepository.save(activeDetail);

        // 4. Kiểm tra và cập nhật trạng thái phiếu mượn cha
        updateParentBorrowStatus(activeDetail.getBorrow());
    }

    // Phê duyệt yêu cầu trả sách trực tuyến từ độc giả
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveOnlineReturn(Integer borrowId) {
        Borrow borrow = borrowRepository.findById(borrowId)
                .orElseThrow(() -> new ResourceNotFoundException(localizedMessageService.get("backend.loan.returnRequestNotFound")));

        List<BorrowDetail> details = borrowDetailRepository.findByBorrowId(borrowId);
        
        for (BorrowDetail detail : details) {
            if ("Return_Pending".equalsIgnoreCase(detail.getStatus()) || STATUS_BORROWED.equalsIgnoreCase(detail.getStatus()) || STATUS_OVERDUE.equalsIgnoreCase(detail.getStatus())) {
                // 1. Cập nhật trạng thái sách vật lý về Available
                if (detail.getBookItem() != null) {
                    BookItem item = detail.getBookItem();
                    item.setStatus(STATUS_AVAILABLE);
                    bookItemRepository.save(item);
                }

                // 2. Tính toán phạt quá hạn nếu có
                processOverdueFine(detail);

                // 3. Cập nhật trạng thái chi tiết mượn
                detail.setReturnDate(LocalDateTime.now());
                detail.setStatus(STATUS_RETURNED);
                borrowDetailRepository.save(detail);
            }
        }

        // 4. Cập nhật trạng thái phiếu mượn cha
        updateParentBorrowStatus(borrow);

        // 5. Gửi thông báo đến độc giả
        sendInternalNotification(borrow.getMember(), localizedMessageService.get("systemNotification.return.approved.title"),
                localizedMessageService.get("systemNotification.return.approved.content", borrowId));
    }

    // UC-13.3: Quầy mượn sách
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processBorrowDesk(String memberIdentifier, List<String> barcodes, String staffUsername) {
        Member member;
        try {
            if (memberIdentifier != null && memberIdentifier.contains("@")) {
                member = memberRepository.findByUserEmail(memberIdentifier)
                        .orElseThrow(() -> new ResourceNotFoundException(localizedMessageService.get("backend.loan.memberEmailNotFound")));
            } else {
                member = memberRepository.findByUserPhone(memberIdentifier)
                        .orElseThrow(() -> new ResourceNotFoundException(localizedMessageService.get("backend.loan.memberPhoneNotFound")));
            }
        } catch (org.springframework.dao.IncorrectResultSizeDataAccessException e) {
            throw new DataProcessingException(localizedMessageService.get("backend.loan.duplicateMemberData"), e);
        }

        Staff staff = staffAccountRepository.findByUsername(staffUsername)
                .map(StaffAccount::getStaff)
                .orElseThrow(() -> new ResourceNotFoundException(localizedMessageService.get("backend.loan.librarianNotFound")));

        List<BookItem> bookItemsToBorrow = new ArrayList<>();
        for (String barcode : barcodes) {
            String trimmedBarcode = barcode.trim();
            if (trimmedBarcode.isEmpty()) continue;

            BookItem item;
            try {
                item = bookItemRepository.findByBarcode(trimmedBarcode)
                        .orElseThrow(() -> new ResourceNotFoundException(localizedMessageService.get("backend.loan.barcodeNotFound", trimmedBarcode)));
            } catch (org.springframework.dao.IncorrectResultSizeDataAccessException e) {
                throw new DataProcessingException(localizedMessageService.get("backend.loan.duplicateBarcode", trimmedBarcode), e);
            }

            if (!STATUS_AVAILABLE.equalsIgnoreCase(item.getStatus())) {
                throw new ConflictException(localizedMessageService.get("backend.loan.barcodeUnavailable", trimmedBarcode));
            }
            bookItemsToBorrow.add(item);
        }

        if (bookItemsToBorrow.isEmpty()) {
            throw new ValidationException(localizedMessageService.get("backend.barcode.noneValid"));
        }

        long currentBorrowCount = borrowDetailRepository.countActiveBorrowedBooks(member.getMemberId());
        int maxLimit = member.getTier() != null ? member.getTier().getBorrowLimit() : 3;
        int totalRequestedBooks = (int) currentBorrowCount + bookItemsToBorrow.size();

        if (totalRequestedBooks > maxLimit) {
            throw new ConflictException(localizedMessageService.get("backend.loan.limitExceeded", maxLimit, currentBorrowCount));
        }

        Borrow borrow = new Borrow();
        borrow.setMember(member);
        borrow.setStaff(staff);
        borrow.setBorrowDate(LocalDateTime.now());
        borrow.setStatus(STATUS_ACTIVE);
        borrow = borrowRepository.save(borrow);

        for (BookItem item : bookItemsToBorrow) {
            BorrowDetail detail = new BorrowDetail();
            detail.setBorrow(borrow);
            detail.setBook(item.getBook());
            detail.setBookItem(item);
            detail.setDueDate(LocalDateTime.now().plusDays(14));
            detail.setStatus(STATUS_BORROWED);
            detail.setRenewCount(0);
            borrowDetailRepository.save(detail);

            item.setStatus(STATUS_BORROWED);
            bookItemRepository.save(item);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processBorrowRequest(Integer borrowId) {
        Borrow borrow = borrowRepository.findById(borrowId)
                .orElseThrow(() -> new ResourceNotFoundException(localizedMessageService.get("backend.loan.requestNotFound")));
        
        borrow.setStatus(STATUS_ACTIVE);
        borrowRepository.save(borrow);

        List<BorrowDetail> details = borrowDetailRepository.findByBorrowId(borrowId);
        for (BorrowDetail detail : details) {
            detail.setStatus(STATUS_BORROWED);
            borrowDetailRepository.save(detail);
            
            if (detail.getBookItem() != null) {
                BookItem item = detail.getBookItem();
                item.setStatus(STATUS_BORROWED);
                bookItemRepository.save(item);
            }
        }
    }

    // UC-13.4: Gia hạn mượn
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processRenewal(Integer borrowDetailId) {
        // ... kept for compatibility if needed, but not used by member anymore ...
        BorrowDetail detail = borrowDetailRepository.findById(borrowDetailId)
                .orElseThrow(() -> new ResourceNotFoundException(localizedMessageService.get("backend.loan.detailNotFound")));

        if (!STATUS_BORROWED.equalsIgnoreCase(detail.getStatus())) {
            throw new ConflictException(localizedMessageService.get("backend.loan.renewBorrowedOnly"));
        }

        // Đọc cấu hình số lần gia hạn tối đa (mặc định là 2)
        int maxRenewals = 2;
        Optional<SystemSetting> setting = systemSettingRepository.findBySettingKey("MAX_RENEWALS");
        if (setting.isPresent()) {
            try {
                maxRenewals = Integer.parseInt(setting.get().getSettingValue());
            } catch (NumberFormatException ignored) { /* Use the documented default. */ }
        }

        if (detail.getRenewCount() >= maxRenewals) {
            throw new ConflictException(localizedMessageService.get("backend.loan.maxRenewals", maxRenewals));
        }

        // Đọc cấu hình số ngày gia hạn thêm mỗi lần (mặc định là 7 ngày)
        int renewDays = 7;
        Optional<SystemSetting> renewDaysSetting = systemSettingRepository.findBySettingKey("RENEW_DAYS");
        if (renewDaysSetting.isPresent()) {
            try {
                renewDays = Integer.parseInt(renewDaysSetting.get().getSettingValue());
            } catch (NumberFormatException ignored) { /* Use the documented default. */ }
        }

        detail.setDueDate(detail.getDueDate().plusDays(renewDays));
        detail.setRenewCount(detail.getRenewCount() + 1);
        borrowDetailRepository.save(detail);

        Member member = detail.getBorrow().getMember();
        if (member != null && member.getMemberId() != null) {
            sendInternalNotification(member, localizedMessageService.get("systemNotification.renewal.success.title"),
                    localizedMessageService.get("systemNotification.renewal.success.content",
                            detail.getBook().getTitle(), renewDays,
                            detail.getDueDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
        }
    }

    // Thêm 3 phương thức này vào thân lớp LoanServiceImpl.java

    @Override
    @Transactional(readOnly = true)
    public List<BorrowDetail> findActiveLoansByBarcode(String barcode) {
        if (barcode == null || barcode.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return borrowDetailRepository.findActiveLoansByBarcode(barcode.trim());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmReturnWithDetails(String barcode, LocalDateTime returnDate, String bookCondition, String damageNote, String staffUsername) {
        List<BorrowDetail> activeLoans = borrowDetailRepository.findActiveLoansByBarcode(barcode.trim());
        if (activeLoans.isEmpty()) {
            throw new ResourceNotFoundException(localizedMessageService.get("backend.loan.unreturnedBarcodeNotFound", barcode));
        }

        BorrowDetail detail = activeLoans.get(0);
        BookItem item = detail.getBookItem();

        if (item != null) {
            item.setStatus(STATUS_AVAILABLE);
            item.setBookCondition(bookCondition != null && !bookCondition.trim().isEmpty() ? bookCondition.trim() : "Tốt");
            item.setDamageNote(damageNote != null && !damageNote.trim().isEmpty() ? damageNote.trim() : null);
            bookItemRepository.save(item);
        }

        String fullConditionNote = bookCondition != null ? bookCondition.trim() : "Tốt";
        if (damageNote != null && !damageNote.trim().isEmpty()) {
            fullConditionNote += " - " + damageNote.trim();
        }
        detail.setReturnDate(returnDate);
        detail.setConditionNote(fullConditionNote);
        detail.setStatus(STATUS_RETURNED);
        borrowDetailRepository.save(detail);

        if (returnDate.isAfter(detail.getDueDate())) {
            processOverdueFine(detail);
        }

        updateParentBorrowStatus(detail.getBorrow());
        sendInternalNotification(detail.getBorrow().getMember(),
                localizedMessageService.get("systemNotification.return.desk.title"),
                localizedMessageService.get("systemNotification.return.desk.content", detail.getBook().getTitle()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmBatchReturnWithDetails(List<String> barcodes, LocalDateTime returnDate, String bookCondition, String damageNote, String staffUsername) {
        if (barcodes == null || barcodes.isEmpty()) {
            return;
        }
        for (String barcode : barcodes) {
            if (barcode != null && !barcode.trim().isEmpty()) {
                confirmReturnWithDetails(barcode.trim(), returnDate, bookCondition, damageNote, staffUsername);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<BorrowDetail> searchActiveLoansByQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }
        String cleanQuery = query.trim();

        List<BorrowDetail> results = new ArrayList<>();

        // 1. Try to search by Email or Phone number of a member
        Optional<Member> memberOpt = memberRepository.findByUserEmail(cleanQuery);
        if (!memberOpt.isPresent()) {
            memberOpt = memberRepository.findByUserPhone(cleanQuery);
        }

        if (memberOpt.isPresent()) {
            Member member = memberOpt.get();
            // Get all current borrow details of this member
            List<BorrowDetail> allMemberDetails = borrowDetailRepository.findCurrentBorrowsByMemberId(member.getMemberId());
            for (BorrowDetail d : allMemberDetails) {
                if (d.getBookItem() != null && ("Borrowed".equalsIgnoreCase(d.getStatus())
                        || "Overdue".equalsIgnoreCase(d.getStatus())
                        || "Return_Pending".equalsIgnoreCase(d.getStatus()))) {
                    results.add(d);
                }
            }
            return results;
        }

        // 2. Otherwise, treat as Barcode or BorrowId
        List<Borrow> matchingBorrows = new ArrayList<>();

        // Check if query is borrowId (with or without 'BOR-' prefix)
        String borrowIdStr = cleanQuery;
        if (borrowIdStr.toUpperCase().startsWith("BOR-")) {
            borrowIdStr = borrowIdStr.substring(4);
        }
        try {
            Integer id = Integer.parseInt(borrowIdStr);
            Optional<Borrow> borrowOpt = borrowRepository.findById(id);
            borrowOpt.ifPresent(matchingBorrows::add);
        } catch (NumberFormatException ignored) {}

        // Check if query is barcode
        List<BorrowDetail> barcodeDetails = borrowDetailRepository.findActiveLoansByBarcode(cleanQuery);
        for (BorrowDetail bd : barcodeDetails) {
            if (bd.getBorrow() != null && !matchingBorrows.contains(bd.getBorrow())) {
                matchingBorrows.add(bd.getBorrow());
            }
        }

        // Collect all active details for matching borrows
        for (Borrow b : matchingBorrows) {
            List<BorrowDetail> details = borrowDetailRepository.findByBorrowId(b.getBorrowId());
            for (BorrowDetail d : details) {
                if (d.getBookItem() != null && ("Borrowed".equalsIgnoreCase(d.getStatus())
                        || "Overdue".equalsIgnoreCase(d.getStatus())
                        || "Return_Pending".equalsIgnoreCase(d.getStatus()))) {
                    if (!results.contains(d)) {
                        results.add(d);
                    }
                }
            }
        }

        return results;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmCollection(Integer borrowId) {
        Borrow borrow = borrowRepository.findById(borrowId)
                .orElseThrow(() -> new IllegalArgumentException(localizedMessageService.get("backend.loan.notFoundById", borrowId)));

        if (!"Approved".equalsIgnoreCase(borrow.getStatus())) {
            throw new IllegalArgumentException(localizedMessageService.get("backend.borrow.notWaitingPickup"));
        }

        borrow.setStatus(STATUS_ACTIVE);
        borrow.setBorrowDate(LocalDateTime.now());
        borrowRepository.save(borrow);

        List<BorrowDetail> details = borrowDetailRepository.findByBorrowId(borrowId);
        for (BorrowDetail detail : details) {
            detail.setStatus(STATUS_BORROWED);
            detail.setDueDate(LocalDateTime.now().plusDays(14));
            borrowDetailRepository.save(detail);

            if (detail.getBookItem() != null) {
                BookItem item = detail.getBookItem();
                item.setStatus(STATUS_BORROWED);
                bookItemRepository.save(item);
            }
        }

        sendInternalNotification(borrow.getMember(),
                localizedMessageService.get("systemNotification.borrow.pickup.title"),
                localizedMessageService.get("systemNotification.borrow.collection.content", borrowId,
                        LocalDateTime.now().plusDays(14)
                                .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BorrowDetail> getTodayReturnedBooks() {
        LocalDateTime startOfDay = java.time.LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = java.time.LocalDate.now().atTime(java.time.LocalTime.MAX);
        return borrowDetailRepository.findReturnedBooksToday(startOfDay, endOfDay);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveRenewal(Integer borrowDetailId, String staffUsername) {
        BorrowDetail detail = borrowDetailRepository.findById(borrowDetailId)
                .orElseThrow(() -> new ResourceNotFoundException(localizedMessageService.get("backend.loan.detailNotFound")));

        if (!"Renew_Pending".equalsIgnoreCase(detail.getStatus())) {
            throw new ConflictException(localizedMessageService.get("backend.loan.renewalNotPending"));
        }

        int renewDays = 7;
        Optional<SystemSetting> renewDaysSetting = systemSettingRepository.findBySettingKey("RENEW_DAYS");
        if (renewDaysSetting.isPresent()) {
            try {
                renewDays = Integer.parseInt(renewDaysSetting.get().getSettingValue());
            } catch (NumberFormatException ignored) { /* Use the documented default. */ }
        }

        detail.setDueDate(detail.getDueDate().plusDays(renewDays));
        detail.setRenewCount(detail.getRenewCount() + 1);
        detail.setStatus(STATUS_BORROWED); // Trở về trạng thái đang mượn
        borrowDetailRepository.save(detail);

        if (detail.getBorrow().getMember() != null) {
            sendInternalNotification(detail.getBorrow().getMember(),
                    localizedMessageService.get("systemNotification.renewal.approved.title"),
                    localizedMessageService.get("systemNotification.renewal.approved.content",
                            detail.getBook().getTitle(),
                            detail.getDueDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectRenewal(Integer borrowDetailId, String staffUsername) {
        BorrowDetail detail = borrowDetailRepository.findById(borrowDetailId)
                .orElseThrow(() -> new ResourceNotFoundException(localizedMessageService.get("backend.loan.detailNotFound")));

        if (!"Renew_Pending".equalsIgnoreCase(detail.getStatus())) {
            throw new ConflictException(localizedMessageService.get("backend.loan.renewalNotPending"));
        }

        // Trả về trạng thái cũ: Kiểm tra nếu đã quá hạn thực tế chưa
        if (detail.getDueDate() != null && detail.getDueDate().isBefore(LocalDateTime.now())) {
            detail.setStatus("Overdue");
        } else {
            detail.setStatus(STATUS_BORROWED);
        }
        borrowDetailRepository.save(detail);

        if (detail.getBorrow().getMember() != null) {
            sendInternalNotification(detail.getBorrow().getMember(),
                    localizedMessageService.get("systemNotification.renewal.rejected.title"),
                    localizedMessageService.get("systemNotification.renewal.rejected.content",
                            detail.getBook().getTitle(),
                            detail.getDueDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<BorrowDetail> getAllPendingRenewals() {
        return borrowDetailRepository.findAll().stream()
                .filter(d -> "Renew_Pending".equalsIgnoreCase(d.getStatus()))
                .toList();
    }

    @Override
    public java.util.List<com.lms.entity.BorrowDetail> getAllBorrowDetails() {
        return borrowDetailRepository.findAllBorrowDetailsWithRelationships();
    }
    // --- Private Helper Methods ---

    /**
     * Kiểm tra quá hạn và tính phí phạt, trừ tiền ví, lưu giao dịch
     */
    private void processOverdueFine(BorrowDetail detail) {
        LocalDateTime dueDate = detail.getDueDate();
        LocalDateTime now = LocalDateTime.now();
        
        if (now.isAfter(dueDate)) {
            long overdueDays = ChronoUnit.DAYS.between(dueDate, now);
            if (overdueDays > 0) {
                // Đơn giá phạt mặc định: 5.000đ/ngày
                BigDecimal fineRate = new BigDecimal("5000");
                
                // Lấy đơn giá phạt cấu hình động từ DB
                Optional<SystemSetting> rateSetting = systemSettingRepository.findBySettingKey("FINE_RATE_PER_DAY");
                if (rateSetting.isPresent()) {
                    try {
                        fineRate = new BigDecimal(rateSetting.get().getSettingValue());
                    } catch (NumberFormatException ignored) { /* Use the documented default. */ }
                }
                
                BigDecimal fineAmount = fineRate.multiply(new BigDecimal(overdueDays));
                Member member = detail.getBorrow().getMember();
                
                // Lấy ví của thành viên
                Wallet wallet = walletRepository.findByMemberMemberId(member.getMemberId())
                        .orElseGet(() -> {
                            Wallet newWallet = new Wallet();
                            newWallet.setMember(member);
                            newWallet.setBalance(BigDecimal.ZERO);
                            return walletRepository.save(newWallet);
                        });

                // Khấu trừ số dư ví (cho phép ví âm)
                wallet.setBalance(wallet.getBalance().subtract(fineAmount));
                walletRepository.save(wallet);

                // Ghi nhận giao dịch phạt
                Transaction transaction = new Transaction();
                transaction.setWallet(wallet);
                transaction.setBorrow(detail.getBorrow());
                transaction.setTransactionType("Fine");
                transaction.setAmount(fineAmount);
                transaction.setTransactionDate(now);
                transaction.setStatus("Completed");
                transactionRepository.save(transaction);

                // Gửi thông báo hệ thống đến độc giả
                sendInternalNotification(member, localizedMessageService.get("systemNotification.overdueFine.title"),
                        localizedMessageService.get("systemNotification.overdueFine.content",
                                fineAmount.setScale(0, java.math.RoundingMode.HALF_UP),
                                detail.getBook().getTitle(), overdueDays));
            }
        }
    }

    /**
     * Cập nhật trạng thái của phiếu mượn cha dựa trên các chi tiết sách đã trả
     */
    private void updateParentBorrowStatus(Borrow borrow) {
        List<BorrowDetail> allDetails = borrowDetailRepository.findByBorrowId(borrow.getBorrowId());
        boolean allReturned = true;
        boolean hasOverdue = false;

        for (BorrowDetail d : allDetails) {
            if (!STATUS_RETURNED.equalsIgnoreCase(d.getStatus())) {
                allReturned = false;
            }
            if (STATUS_OVERDUE.equalsIgnoreCase(d.getStatus())) {
                hasOverdue = true;
            }
        }

        if (allReturned) {
            borrow.setStatus(STATUS_RETURNED);
        } else if (hasOverdue) {
            borrow.setStatus(STATUS_OVERDUE);
        } else {
            borrow.setStatus(STATUS_ACTIVE);
        }
        
        borrowRepository.save(borrow);
    }

    /**
     * Tạo thông báo hệ thống và gửi cho độc giả
     */
    private void sendInternalNotification(Member member, String title, String content) {
        if (member == null || member.getMemberId() == null) {
            throw new DataProcessingException(localizedMessageService.get("backend.notification.memberMissing"));
        }
        Notification notif = new Notification();
        notif.setTitle(title);
        notif.setContent(content);
        notif.setCreatedDate(LocalDateTime.now());
        notif.setStatus(STATUS_ACTIVE);
        Notification saved = notificationRepository.save(notif);

        MemberNotification mn = new MemberNotification();
        mn.setId(new MemberNotificationId(member.getMemberId(), saved.getNotificationId()));
        mn.setMember(member);
        mn.setNotification(saved);
        mn.setIsRead(false);
        memberNotificationRepository.save(mn);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BorrowDetail> getMemberBorrowDetailsLimit365Days(Integer memberId) {
        java.time.LocalDateTime limitDate = java.time.LocalDateTime.now().minusDays(365);
        return borrowDetailRepository.findBorrowHistoryLimit365Days(memberId, limitDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Transaction> getMemberTransactionsLimit365Days(Integer memberId) {
        java.time.LocalDateTime limitDate = java.time.LocalDateTime.now().minusDays(365);
        return transactionRepository.findTransactionsByMemberIdLimit365Days(memberId, limitDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BorrowDetail> getMemberBorrowDetailsByDateRange(Integer memberId, java.time.LocalDateTime startDate, java.time.LocalDateTime endDate) {
        return borrowDetailRepository.findBorrowHistoryByDateRange(memberId, startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Transaction> getMemberTransactionsByDateRange(Integer memberId, java.time.LocalDateTime startDate, java.time.LocalDateTime endDate) {
        return transactionRepository.findTransactionsByMemberIdAndDateRange(memberId, startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BorrowDetail> getBorrowSchedule(String borrowDate, String returnDate, String keyword) {
        List<BorrowDetail> all = borrowDetailRepository.findAll();
        return all.stream().filter(d -> {
            boolean match = true;
            if (keyword != null && !keyword.isEmpty()) {
                match = (d.getBook() != null && d.getBook().getTitle().toLowerCase().contains(keyword.toLowerCase()))
                        || (d.getBookItem() != null && d.getBookItem().getBarcode().contains(keyword));
            }
            return match;
        }).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmBookReturn(Integer borrowDetailId, String conditionNote) {
        BorrowDetail detail = borrowDetailRepository.findById(borrowDetailId)
                .orElseThrow(() -> new IllegalArgumentException(localizedMessageService.get("backend.loan.detailNotFound")));

        BookItem item = detail.getBookItem();
        if (item == null) {
            throw new IllegalArgumentException(localizedMessageService.get("backend.loan.physicalCopyMissing"));
        }
        String barcode = item.getBarcode();
        if (barcode == null || barcode.trim().isEmpty()) {
            throw new IllegalArgumentException(localizedMessageService.get("backend.loan.physicalBarcodeMissing"));
        }
        confirmReturnWithDetails(barcode, LocalDateTime.now(), conditionNote, null, "admin");
    }

    @Override
    @Transactional(readOnly = true)
    public List<BorrowDetail> getBorrowDetailsByBorrowId(Integer borrowId) {
        return borrowDetailRepository.findByBorrowId(borrowId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Transaction> getTransactionsByBorrowId(Integer borrowId) {
        return transactionRepository.findByBorrow_BorrowId(borrowId);
    }
}
