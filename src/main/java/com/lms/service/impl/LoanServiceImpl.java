package com.lms.service.impl;

import com.lms.entity.*;
import com.lms.repository.*;
import com.lms.service.LoanService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class LoanServiceImpl implements LoanService {

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

    /**
     * Helper Method: Tính toán chi phí mượn sách, khấu trừ ví và ghi nhận giao dịch âm
     */
    private BigDecimal deductBorrowingFee(Borrow borrow, int bookCount, int borrowDays) {
        Member member = borrow.getMember();
        Wallet wallet = walletRepository.findByMemberMemberId(member.getMemberId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ví điện tử của độc giả!"));

        // Đọc cấu hình phí cơ bản
        BigDecimal feePerBookPerDay = BigDecimal.valueOf(5000);
        Optional<SystemSetting> setting = systemSettingRepository.findBySettingKey("FEE_PER_BOOK_PER_DAY");
        if (setting.isPresent()) {
            try {
                feePerBookPerDay = new BigDecimal(setting.get().getSettingValue());
            } catch (Exception ignored) {}
        }

        // Tính chiết khấu theo hạng thành viên
        double discountPercent = (member.getTier() != null && member.getTier().getDiscountPercent() != null)
                ? member.getTier().getDiscountPercent().doubleValue() : 0.0;
        BigDecimal discountFactor = BigDecimal.valueOf(1.0 - (discountPercent / 100.0));

        BigDecimal totalFee = feePerBookPerDay
                .multiply(BigDecimal.valueOf(bookCount))
                .multiply(BigDecimal.valueOf(borrowDays))
                .multiply(discountFactor);

        if (wallet.getBalance().compareTo(totalFee) < 0) {
            throw new IllegalArgumentException("Số dư ví của độc giả không đủ! Yêu cầu: "
                    + totalFee + " VND, hiện tại: " + wallet.getBalance() + " VND.");
        }

        // Trừ số dư ví
        wallet.setBalance(wallet.getBalance().subtract(totalFee));
        walletRepository.save(wallet);

        // Lưu log giao dịch tài chính với giá trị âm (tiền ra khỏi ví)
        Transaction feeTxn = new Transaction();
        feeTxn.setWallet(wallet);
        feeTxn.setBorrow(borrow);
        feeTxn.setTransactionType("BORROW_FEE");
        feeTxn.setAmount(totalFee.negate()); // Lưu số âm
        feeTxn.setTransactionDate(LocalDateTime.now());
        feeTxn.setStatus("Completed");
        transactionRepository.save(feeTxn);

        return totalFee;
    }

    @Override
    @Transactional(readOnly = true)
    public Borrow getLoanDetails(Integer borrowId) {
        return borrowRepository.findById(borrowId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu mượn với mã: " + borrowId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmReturn(String barcode) {
        BookItem item = bookItemRepository.findByBarcode(barcode)
                .orElseThrow(() -> new IllegalArgumentException("Mã vạch sách vật lý '" + barcode + "' không tồn tại!"));

        BorrowDetail activeDetail = borrowDetailRepository.findAll().stream()
                .filter(d -> d.getBookItem() != null && d.getBookItem().getBookItemId().equals(item.getBookItemId())
                        && (STATUS_BORROWED.equalsIgnoreCase(d.getStatus()) || STATUS_OVERDUE.equalsIgnoreCase(d.getStatus()) || "Return_Pending".equalsIgnoreCase(d.getStatus())))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lịch sử mượn hợp lệ ứng với mã vạch sách này!"));

        item.setStatus(STATUS_AVAILABLE);
        bookItemRepository.save(item);

        processOverdueFine(activeDetail);

        activeDetail.setReturnDate(LocalDateTime.now());
        activeDetail.setStatus(STATUS_RETURNED);
        borrowDetailRepository.save(activeDetail);

        updateParentBorrowStatus(activeDetail.getBorrow());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveOnlineReturn(Integer borrowId) {
        Borrow borrow = borrowRepository.findById(borrowId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn yêu cầu trả!"));

        List<BorrowDetail> details = borrowDetailRepository.findByBorrowId(borrowId);

        for (BorrowDetail detail : details) {
            if ("Return_Pending".equalsIgnoreCase(detail.getStatus()) || STATUS_BORROWED.equalsIgnoreCase(detail.getStatus()) || STATUS_OVERDUE.equalsIgnoreCase(detail.getStatus())) {
                if (detail.getBookItem() != null) {
                    BookItem item = detail.getBookItem();
                    item.setStatus(STATUS_AVAILABLE);
                    bookItemRepository.save(item);
                }
                processOverdueFine(detail);
                detail.setReturnDate(LocalDateTime.now());
                detail.setStatus(STATUS_RETURNED);
                borrowDetailRepository.save(detail);
            }
        }
        updateParentBorrowStatus(borrow);
        sendInternalNotification(borrow.getMember(), "Xác nhận trả sách thành công",
                "Yêu cầu trả sách của phiếu mượn #" + borrowId + " đã được thủ thư phê duyệt.");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processBorrowDesk(String memberIdentifier, List<String> barcodes, String staffUsername) {
        Member member;
        try {
            if (memberIdentifier != null && memberIdentifier.contains("@")) {
                member = memberRepository.findByUserEmail(memberIdentifier)
                        .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thành viên với Email này!"));
            } else {
                member = memberRepository.findByUserPhone(memberIdentifier)
                        .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thành viên với số điện thoại này!"));
            }
        } catch (org.springframework.dao.IncorrectResultSizeDataAccessException e) {
            throw new IllegalArgumentException("Có nhiều độc giả trùng thông tin này! Vui lòng liên hệ quản trị viên.");
        }

        Staff staff = staffAccountRepository.findByUsername(staffUsername)
                .map(StaffAccount::getStaff)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin thủ thư!"));

        List<BookItem> bookItemsToBorrow = new ArrayList<>();
        for (String barcode : barcodes) {
            String trimmedBarcode = barcode.trim();
            if (trimmedBarcode.isEmpty()) continue;

            BookItem item = bookItemRepository.findByBarcode(trimmedBarcode)
                    .orElseThrow(() -> new IllegalArgumentException("Mã vạch " + trimmedBarcode + " không tồn tại!"));

            if (!STATUS_AVAILABLE.equalsIgnoreCase(item.getStatus())) {
                throw new IllegalArgumentException("Sách có mã vạch " + trimmedBarcode + " hiện tại không sẵn sàng!");
            }
            bookItemsToBorrow.add(item);
        }

        if (bookItemsToBorrow.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập ít nhất 1 mã vạch hợp lệ!");
        }

        long currentBorrowCount = borrowDetailRepository.countActiveBorrowedBooks(member.getMemberId());
        int maxLimit = member.getTier() != null ? member.getTier().getBorrowLimit() : 3;
        int totalRequestedBooks = (int) currentBorrowCount + bookItemsToBorrow.size();

        if (totalRequestedBooks > maxLimit) {
            throw new IllegalArgumentException("Số lượng sách vượt quá giới hạn mượn! (Tối đa " + maxLimit + " cuốn)");
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

        // Tích hợp thanh toán phí mượn trực tiếp tại quầy
        deductBorrowingFee(borrow, bookItemsToBorrow.size(), 14);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processBorrowRequest(Integer borrowId) {
        Borrow borrow = borrowRepository.findById(borrowId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy yêu cầu mượn!"));

        List<BorrowDetail> details = borrowDetailRepository.findByBorrowId(borrowId);

        int borrowDays = 14;
        if (!details.isEmpty() && details.get(0).getDueDate() != null) {
            long days = ChronoUnit.DAYS.between(borrow.getBorrowDate(), details.get(0).getDueDate());
            borrowDays = (int) Math.max(1, days);
        }

        // Tích hợp thanh toán phí mượn khi phê duyệt đơn đặt trực tuyến
        BigDecimal chargeAmount = deductBorrowingFee(borrow, details.size(), borrowDays);

        borrow.setStatus("Approved");
        borrow.setBorrowDate(LocalDateTime.now()); // Reset borrowDate to approval time for the 48h collect limit
        borrowRepository.save(borrow);

        for (BorrowDetail detail : details) {
            detail.setStatus("Approved");

            if (detail.getBookItem() == null) {
                List<BookItem> items = bookItemRepository.findByBook_BookId(detail.getBook().getBookId());
                BookItem availableItem = items.stream()
                        .filter(item -> "Available".equalsIgnoreCase(item.getStatus()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Không còn bản sách vật lý nào sẵn sàng cho cuốn sách: " + detail.getBook().getTitle()));
                detail.setBookItem(availableItem);
                availableItem.setStatus("Approved");
                bookItemRepository.save(availableItem);
            } else {
                BookItem item = detail.getBookItem();
                item.setStatus("Approved");
                bookItemRepository.save(item);
            }
            borrowDetailRepository.save(detail);
        }

        sendInternalNotification(borrow.getMember(), "Yêu cầu mượn sách được phê duyệt",
                "Yêu cầu mượn sách #" + borrowId + " đã được phê duyệt. Vui lòng đến thư viện nhận sách vật lý trong vòng 48 giờ. Phí mượn "
                        + chargeAmount + " VND đã được khấu trừ vào ví điện tử.");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmCollection(Integer borrowId) {
        Borrow borrow = borrowRepository.findById(borrowId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu mượn!"));

        if (!"Approved".equalsIgnoreCase(borrow.getStatus())) {
            throw new IllegalArgumentException("Phiếu mượn này không ở trạng thái Chờ nhận bản vật lý!");
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

        sendInternalNotification(borrow.getMember(), "Đã nhận sách vật lý thành công",
                "Bạn đã nhận sách vật lý cho phiếu mượn #" + borrowId + " thành công. Hạn trả sách của bạn là: "
                + LocalDateTime.now().plusDays(14).format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ".");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processRenewal(Integer borrowDetailId) {
        BorrowDetail detail = borrowDetailRepository.findById(borrowDetailId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chi tiết phiếu mượn!"));

        if (!STATUS_BORROWED.equalsIgnoreCase(detail.getStatus())) {
            throw new IllegalArgumentException("Chỉ sách đang ở trạng thái 'Đang mượn' mới được phép gia hạn!");
        }

        int maxRenewals = 2;
        Optional<SystemSetting> setting = systemSettingRepository.findBySettingKey("MAX_RENEWALS");
        if (setting.isPresent()) {
            try {
                maxRenewals = Integer.parseInt(setting.get().getSettingValue());
            } catch (Exception ignored) { }
        }

        if (detail.getRenewCount() >= maxRenewals) {
            throw new IllegalArgumentException("Sách này đã được gia hạn tối đa " + maxRenewals + " lần!");
        }

        int renewDays = 7;
        Optional<SystemSetting> renewDaysSetting = systemSettingRepository.findBySettingKey("RENEW_DAYS");
        if (renewDaysSetting.isPresent()) {
            try {
                renewDays = Integer.parseInt(renewDaysSetting.get().getSettingValue());
            } catch (Exception ignored) { }
        }

        detail.setDueDate(detail.getDueDate().plusDays(renewDays));
        detail.setRenewCount(detail.getRenewCount() + 1);
        borrowDetailRepository.save(detail);

        try {
            Member member = detail.getBorrow().getMember();
            if (member != null && member.getMemberId() != null) {
                Notification notif = new Notification();
                notif.setTitle("Gia hạn sách thành công");
                notif.setContent("Cuốn sách '" + detail.getBook().getTitle() + "' đã được gia hạn thêm " + renewDays + " ngày.");
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
        } catch (Exception e) { }
    }

    @Override
    @Transactional(readOnly = true)
    public List<BorrowDetail> findActiveLoansByBarcode(String barcode) {
        if (barcode == null || barcode.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return borrowDetailRepository.findActiveLoansByBarcode(barcode.trim());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BorrowDetail> searchActiveLoansByQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }
        String cleanQuery = query.trim();
        if (cleanQuery.toUpperCase().startsWith("BOR-")) {
            cleanQuery = cleanQuery.substring(4);
        }

        List<Borrow> matchingBorrows = new ArrayList<>();

        // 1. Try to search by borrowId
        try {
            Integer id = Integer.parseInt(cleanQuery);
            Optional<Borrow> borrowOpt = borrowRepository.findById(id);
            borrowOpt.ifPresent(matchingBorrows::add);
        } catch (NumberFormatException ignored) {}

        // 2. Try to search by barcode
        List<BorrowDetail> barcodeDetails = borrowDetailRepository.findActiveLoansByBarcode(cleanQuery);
        for (BorrowDetail bd : barcodeDetails) {
            if (bd.getBorrow() != null && !matchingBorrows.contains(bd.getBorrow())) {
                matchingBorrows.add(bd.getBorrow());
            }
        }

        // 3. Try to search by phone number
        List<Borrow> phoneBorrows = borrowRepository.findByMember_User_Phone(cleanQuery);
        for (Borrow b : phoneBorrows) {
            if (!matchingBorrows.contains(b)) {
                matchingBorrows.add(b);
            }
        }

        // Collect all active details for these borrows
        List<BorrowDetail> results = new ArrayList<>();
        for (Borrow b : matchingBorrows) {
            List<BorrowDetail> details = borrowDetailRepository.findByBorrowId(b.getBorrowId());
            for (BorrowDetail d : details) {
                if ("Borrowed".equalsIgnoreCase(d.getStatus())
                        || "Overdue".equalsIgnoreCase(d.getStatus())
                        || "Return_Pending".equalsIgnoreCase(d.getStatus())) {
                    results.add(d);
                }
            }
        }

        return results;
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
    @Transactional(rollbackFor = Exception.class)
    public void confirmReturnWithDetails(String barcode, LocalDateTime returnDate, String bookCondition, String damageNote, String staffUsername) {
        List<BorrowDetail> activeLoans = borrowDetailRepository.findActiveLoansByBarcode(barcode.trim());
        if (activeLoans.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy lượt mượn nào chưa trả ứng với mã vạch: " + barcode);
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
        sendInternalNotification(detail.getBorrow().getMember(), "Xác nhận hoàn trả sách tại quầy thành công",
                "Cuốn sách '" + detail.getBook().getTitle() + "' đã được thủ thư tiếp nhận nhập kho tại quầy.");
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
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chi tiết phiếu mượn!"));

        if (!"Renew_Pending".equalsIgnoreCase(detail.getStatus())) {
            throw new IllegalArgumentException("Yêu cầu này không ở trạng thái chờ duyệt gia hạn!");
        }

        int renewDays = 7;
        Optional<SystemSetting> renewDaysSetting = systemSettingRepository.findBySettingKey("RENEW_DAYS");
        if (renewDaysSetting.isPresent()) {
            try {
                renewDays = Integer.parseInt(renewDaysSetting.get().getSettingValue());
            } catch (Exception ignored) { }
        }

        detail.setDueDate(detail.getDueDate().plusDays(renewDays));
        detail.setRenewCount(detail.getRenewCount() + 1);
        detail.setStatus(STATUS_BORROWED);
        borrowDetailRepository.save(detail);

        if (detail.getBorrow().getMember() != null) {
            sendInternalNotification(detail.getBorrow().getMember(), "Phê duyệt gia hạn thành công",
                    "Yêu cầu gia hạn sách '" + detail.getBook().getTitle() + "' đã được phê duyệt.");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectRenewal(Integer borrowDetailId, String staffUsername) {
        BorrowDetail detail = borrowDetailRepository.findById(borrowDetailId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chi tiết phiếu mượn!"));

        if (!"Renew_Pending".equalsIgnoreCase(detail.getStatus())) {
            throw new IllegalArgumentException("Yêu cầu này không ở trạng thái chờ duyệt gia hạn!");
        }

        if (detail.getDueDate() != null && detail.getDueDate().isBefore(LocalDateTime.now())) {
            detail.setStatus("Overdue");
        } else {
            detail.setStatus(STATUS_BORROWED);
        }
        borrowDetailRepository.save(detail);

        if (detail.getBorrow().getMember() != null) {
            sendInternalNotification(detail.getBorrow().getMember(), "Từ chối gia hạn sách",
                    "Yêu cầu gia hạn sách '" + detail.getBook().getTitle() + "' đã bị từ chối.");
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
    public List<BorrowDetail> getAllBorrowDetails() {
        return borrowDetailRepository.findAll();
    }

    private void processOverdueFine(BorrowDetail detail) {
        LocalDateTime dueDate = detail.getDueDate();
        LocalDateTime now = LocalDateTime.now();

        if (now.isAfter(dueDate)) {
            long overdueDays = ChronoUnit.DAYS.between(dueDate, now);
            if (overdueDays > 0) {
                BigDecimal fineRate = new BigDecimal("5000");
                Optional<SystemSetting> rateSetting = systemSettingRepository.findBySettingKey("FINE_RATE_PER_DAY");
                if (rateSetting.isPresent()) {
                    try {
                        fineRate = new BigDecimal(rateSetting.get().getSettingValue());
                    } catch (Exception ignored) { }
                }

                BigDecimal fineAmount = fineRate.multiply(new BigDecimal(overdueDays));
                Member member = detail.getBorrow().getMember();

                Wallet wallet = walletRepository.findByMemberMemberId(member.getMemberId())
                        .orElseGet(() -> {
                            Wallet newWallet = new Wallet();
                            newWallet.setMember(member);
                            newWallet.setBalance(BigDecimal.ZERO);
                            return walletRepository.save(newWallet);
                        });

                wallet.setBalance(wallet.getBalance().subtract(fineAmount));
                walletRepository.save(wallet);

                Transaction transaction = new Transaction();
                transaction.setWallet(wallet);
                transaction.setBorrow(detail.getBorrow());
                transaction.setTransactionType("Fine");
                transaction.setAmount(fineAmount.negate()); // Lưu dạng số âm nhất quán cho tiền phạt ra khỏi ví
                transaction.setTransactionDate(now);
                transaction.setStatus("Completed");
                transactionRepository.save(transaction);

                sendInternalNotification(member, "Phạt quá hạn trả sách",
                        String.format("Hệ thống ghi nhận khoản phạt quá hạn %sđ đối với sách '%s'.",
                                fineAmount.setScale(0, java.math.RoundingMode.HALF_UP), detail.getBook().getTitle()));
            }
        }
    }

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

    private void sendInternalNotification(Member member, String title, String content) {
        try {
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
        } catch (Exception ignored) { }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmBookReturn(Integer borrowDetailId, String conditionNote) {
        BorrowDetail detail = borrowDetailRepository.findById(borrowDetailId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chi tiết phiếu mượn"));

        BookItem item = detail.getBookItem();
        if (item == null) {
            throw new IllegalArgumentException("Lượt mượn này không liên kết với cuốn sách vật lý nào!");
        }
        String barcode = item.getBarcode();
        if (barcode == null || barcode.trim().isEmpty()) {
            throw new IllegalArgumentException("Sách vật lý liên kết không có mã vạch hợp lệ!");
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
}