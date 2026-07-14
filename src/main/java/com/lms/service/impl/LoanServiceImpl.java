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

/**
 * LoanServiceImpl - Xử lý Logic Quản lý Phiếu mượn (Thủ thư)
 * Người phụ trách: Huỳnh Gia Hưng (CE190488)
 */
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

    // UC-13.1: Xem chi tiết phiếu mượn
    @Override
    @Transactional(readOnly = true)
    public Borrow getLoanDetails(Integer borrowId) {
        return borrowRepository.findById(borrowId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu mượn với mã: " + borrowId));
    }

    // UC-13.2: Xác nhận trả sách trực tiếp bằng quét mã vạch (Barcode) tại quầy
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
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn yêu cầu trả!"));

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
        sendInternalNotification(borrow.getMember(), "Xác nhận trả sách thành công",
                "Yêu cầu trả sách của phiếu mượn #" + borrowId + " đã được thủ thư phê duyệt.");
    }

    // UC-13.3: Quầy mượn sách
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
            throw new IllegalArgumentException("Có nhiều độc giả trùng thông tin này! Vui lòng liên hệ quản trị viên để xử lý dữ liệu trùng lặp.");
        }

        Staff staff = staffAccountRepository.findByUsername(staffUsername)
                .map(StaffAccount::getStaff)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin thủ thư!"));

        List<BookItem> bookItemsToBorrow = new ArrayList<>();
        for (String barcode : barcodes) {
            String trimmedBarcode = barcode.trim();
            if (trimmedBarcode.isEmpty()) continue;

            BookItem item;
            try {
                item = bookItemRepository.findByBarcode(trimmedBarcode)
                        .orElseThrow(() -> new IllegalArgumentException("Mã vạch " + trimmedBarcode + " không tồn tại!"));
            } catch (org.springframework.dao.IncorrectResultSizeDataAccessException e) {
                throw new IllegalArgumentException("Lỗi CSDL: Có nhiều cuốn sách cùng sử dụng mã vạch " + trimmedBarcode + ". Vui lòng liên hệ Admin!");
            }

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
            throw new IllegalArgumentException("Số lượng sách vượt quá giới hạn mượn của thành viên! (Tối đa " + maxLimit + " cuốn, đang mượn " + currentBorrowCount + " cuốn)");
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
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy yêu cầu mượn!"));
        
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
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chi tiết phiếu mượn!"));

        if (!STATUS_BORROWED.equalsIgnoreCase(detail.getStatus())) {
            throw new IllegalArgumentException("Chỉ sách đang ở trạng thái 'Đang mượn' mới được phép gia hạn!");
        }

        // Đọc cấu hình số lần gia hạn tối đa (mặc định là 2)
        int maxRenewals = 2;
        Optional<SystemSetting> setting = systemSettingRepository.findBySettingKey("MAX_RENEWALS");
        if (setting.isPresent()) {
            try {
                maxRenewals = Integer.parseInt(setting.get().getSettingValue());
            } catch (Exception ignored) { /* Ignored by design */ }
        }

        if (detail.getRenewCount() >= maxRenewals) {
            throw new IllegalArgumentException("Sách này đã được gia hạn tối đa " + maxRenewals + " lần!");
        }

        // Đọc cấu hình số ngày gia hạn thêm mỗi lần (mặc định là 7 ngày)
        int renewDays = 7;
        Optional<SystemSetting> renewDaysSetting = systemSettingRepository.findBySettingKey("RENEW_DAYS");
        if (renewDaysSetting.isPresent()) {
            try {
                renewDays = Integer.parseInt(renewDaysSetting.get().getSettingValue());
            } catch (Exception ignored) { /* Ignored by design */ }
        }

        detail.setDueDate(detail.getDueDate().plusDays(renewDays));
        detail.setRenewCount(detail.getRenewCount() + 1);
        borrowDetailRepository.save(detail);

        try {
            Member member = detail.getBorrow().getMember();
            if (member != null && member.getMemberId() != null) {
                Notification notif = new Notification();
                notif.setTitle("Gia hạn sách thành công");
                notif.setContent("Cuốn sách '" + detail.getBook().getTitle() + "' đã được gia hạn thêm " + renewDays + " ngày. Hạn trả mới của bạn là: " + detail.getDueDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ".");
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
        } catch (Exception e) {
            // Log if needed
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
    public void confirmReturnWithDetails(String barcode, LocalDateTime returnDate, String conditionNote, String staffUsername) {
        List<BorrowDetail> activeLoans = borrowDetailRepository.findActiveLoansByBarcode(barcode.trim());
        if (activeLoans.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy lượt mượn nào chưa trả ứng với mã vạch: " + barcode);
        }

        // Xử lý lượt mượn tìm thấy hợp lệ
        BorrowDetail detail = activeLoans.get(0);
        BookItem item = detail.getBookItem();

        // 1. Cập nhật trạng thái sách vật lý về Available
        if (item != null) {
            item.setStatus(STATUS_AVAILABLE);
            bookItemRepository.save(item);
        }

        // 2. Lưu thông tin ngày trả, ghi chú tình trạng ngoại quan và cập nhật status
        detail.setReturnDate(returnDate);
        detail.setConditionNote(conditionNote);
        detail.setStatus(STATUS_RETURNED);
        borrowDetailRepository.save(detail);

        // 3. Kế thừa hàm tính phạt quá hạn có sẵn của nhóm bạn nếu ngày trả thực tế vượt hạn
        if (returnDate.isAfter(detail.getDueDate())) {
            processOverdueFine(detail);
        }

        // 4. Cập nhật trạng thái của phiếu mượn tổng cha (Borrow)
        updateParentBorrowStatus(detail.getBorrow());

        // 5. Gửi thông báo đến độc giả về việc trả sách tại quầy thành công
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
        detail.setStatus(STATUS_BORROWED); // Trở về trạng thái đang mượn
        borrowDetailRepository.save(detail);

        if (detail.getBorrow().getMember() != null) {
            sendInternalNotification(detail.getBorrow().getMember(),
                    "Phê duyệt gia hạn thành công",
                    "Yêu cầu gia hạn sách '" + detail.getBook().getTitle() + "' đã được thủ thư phê duyệt. Hạn trả mới: " + detail.getDueDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
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

        // Trả về trạng thái cũ: Kiểm tra nếu đã quá hạn thực tế chưa
        if (detail.getDueDate() != null && detail.getDueDate().isBefore(LocalDateTime.now())) {
            detail.setStatus("Overdue");
        } else {
            detail.setStatus(STATUS_BORROWED);
        }
        borrowDetailRepository.save(detail);

        if (detail.getBorrow().getMember() != null) {
            sendInternalNotification(detail.getBorrow().getMember(),
                    "Từ chối gia hạn sách",
                    "Yêu cầu gia hạn sách '" + detail.getBook().getTitle() + "' đã bị từ chối. Vui lòng trả sách đúng hạn: " + detail.getDueDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
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
        // Thay thế phương thức không tồn tại bằng findAll() mặc định
        return borrowDetailRepository.findAll();
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
                    } catch (Exception ignored) { /* Ignored by design */ }
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
                sendInternalNotification(member, "Phạt quá hạn trả sách", 
                        String.format("Hệ thống đã ghi nhận khoản phạt quá hạn %sđ đối với sách '%s' (Trễ %d ngày).", 
                                fineAmount.setScale(0, java.math.RoundingMode.HALF_UP), detail.getBook().getTitle(), overdueDays));
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
        } catch (Exception ignored) { /* Ignored by design */ }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmBookReturn(Integer borrowDetailId, String conditionNote) {
        BorrowDetail detail = borrowDetailRepository.findById(borrowDetailId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chi tiết phiếu mượn"));

        // Lấy barcode an toàn từ detail
        BookItem item = detail.getBookItem();
        if (item == null) {
            throw new IllegalArgumentException("Lượt mượn này không liên kết với cuốn sách vật lý nào!");
        }
        String barcode = item.getBarcode();
        if (barcode == null || barcode.trim().isEmpty()) {
            throw new IllegalArgumentException("Sách vật lý liên kết không có mã vạch hợp lệ!");
        }
        confirmReturnWithDetails(barcode, LocalDateTime.now(), conditionNote, "admin");
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
        // Nếu bạn có repository riêng để lọc thì dùng, nếu không dùng cách lọc qua list tất cả
        List<BorrowDetail> all = borrowDetailRepository.findAll();

        // Ví dụ lọc đơn giản (bạn có thể thay thế bằng query JPA tối ưu hơn sau)
        return all.stream().filter(d -> {
            boolean match = true;
            if (keyword != null && !keyword.isEmpty()) {
                match = (d.getBook() != null && d.getBook().getTitle().toLowerCase().contains(keyword.toLowerCase()))
                        || (d.getBookItem() != null && d.getBookItem().getBarcode().contains(keyword));
            }
            return match;
        }).toList();
    }
}
