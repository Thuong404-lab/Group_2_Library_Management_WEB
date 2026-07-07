package com.lms.service.impl;

import com.lms.service.LoanService;
import com.lms.entity.*;
import com.lms.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * LoanServiceImpl - Xử lý Logic Quản lý Phiếu mượn (Thủ thư)
 * Người phụ trách: Huỳnh Gia Hưng (CE190488)
 */
@Service
public class LoanServiceImpl implements LoanService {
    private final BorrowRepository borrowRepository;
    private final BorrowDetailRepository borrowDetailRepository;
    private final BookItemRepository bookItemRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final SystemSettingRepository systemSettingRepository;
    private final NotificationRepository notificationRepository;
    private final MemberNotificationRepository memberNotificationRepository;
    private final ReservationRepository reservationRepository;

    public LoanServiceImpl(BorrowRepository borrowRepository,
                           BorrowDetailRepository borrowDetailRepository,
                           BookItemRepository bookItemRepository,
                           WalletRepository walletRepository,
                           TransactionRepository transactionRepository,
                           SystemSettingRepository systemSettingRepository,
                           NotificationRepository notificationRepository,
                           MemberNotificationRepository memberNotificationRepository,
                           ReservationRepository reservationRepository) {
        this.borrowRepository = borrowRepository;
        this.borrowDetailRepository = borrowDetailRepository;
        this.bookItemRepository = bookItemRepository;
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.systemSettingRepository = systemSettingRepository;
        this.notificationRepository = notificationRepository;
        this.memberNotificationRepository = memberNotificationRepository;
        this.reservationRepository = reservationRepository;
    }

    // UC-13.1: Xem chi tiết phiếu mượn
    @Override
    @Transactional(readOnly = true)
    public Borrow getLoanDetails(Integer borrowId) throws Exception {
        return borrowRepository.findById(borrowId)
                .orElseThrow(() -> new Exception("Không tìm thấy phiếu mượn với mã: " + borrowId));
    }

    // UC-13.2: Xác nhận trả sách trực tiếp bằng quét mã vạch (Barcode) tại quầy
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmReturn(String barcode) throws Exception {
        BookItem item = bookItemRepository.findByBarcode(barcode)
                .orElseThrow(() -> new Exception("Mã vạch sách vật lý '" + barcode + "' không tồn tại!"));

        BorrowDetail activeDetail = borrowDetailRepository.findAll().stream()
                .filter(d -> d.getBookItem() != null && d.getBookItem().getBookItemId().equals(item.getBookItemId())
                        && ("Borrowed".equalsIgnoreCase(d.getStatus()) || "Overdue".equalsIgnoreCase(d.getStatus()) || "Return_Pending".equalsIgnoreCase(d.getStatus())))
                .findFirst()
                .orElseThrow(() -> new Exception("Không tìm thấy lịch sử mượn hợp lệ ứng với mã vạch sách này!"));

        // 1. Cập nhật trạng thái sách vật lý
        item.setStatus("Available");
        bookItemRepository.save(item);

        // 2. Tính toán phạt quá hạn nếu có
        processOverdueFine(activeDetail);

        // 3. Cập nhật trạng thái chi tiết mượn
        activeDetail.setReturnDate(LocalDateTime.now());
        activeDetail.setStatus("Returned");
        borrowDetailRepository.save(activeDetail);

        // 4. Kiểm tra và cập nhật trạng thái phiếu mượn cha
        updateParentBorrowStatus(activeDetail.getBorrow());
    }

    // Phê duyệt yêu cầu trả sách trực tuyến từ độc giả
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveOnlineReturn(Integer borrowId) throws Exception {
        Borrow borrow = borrowRepository.findById(borrowId)
                .orElseThrow(() -> new Exception("Không tìm thấy đơn yêu cầu trả!"));

        List<BorrowDetail> details = borrowDetailRepository.findByBorrowId(borrowId);
        
        for (BorrowDetail detail : details) {
            if ("Return_Pending".equalsIgnoreCase(detail.getStatus()) || "Borrowed".equalsIgnoreCase(detail.getStatus()) || "Overdue".equalsIgnoreCase(detail.getStatus())) {
                // 1. Cập nhật trạng thái sách vật lý về Available
                if (detail.getBookItem() != null) {
                    BookItem item = detail.getBookItem();
                    item.setStatus("Available");
                    bookItemRepository.save(item);
                }

                // 2. Tính toán phạt quá hạn nếu có
                processOverdueFine(detail);

                // 3. Cập nhật trạng thái chi tiết mượn
                detail.setReturnDate(LocalDateTime.now());
                detail.setStatus("Returned");
                borrowDetailRepository.save(detail);
            }
        }

        // 4. Cập nhật trạng thái phiếu mượn cha
        updateParentBorrowStatus(borrow);

        // 5. Gửi thông báo đến độc giả
        sendInternalNotification(borrow.getMember(), "Xác nhận trả sách thành công",
                "Yêu cầu trả sách của phiếu mượn #" + borrowId + " đã được thủ thư phê duyệt.");
    }

    // UC-13.3: Duyệt yêu cầu mượn
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processBorrowRequest(Integer borrowId) throws Exception {
        Borrow borrow = borrowRepository.findById(borrowId)
                .orElseThrow(() -> new Exception("Không tìm thấy yêu cầu mượn!"));
        
        borrow.setStatus("Active");
        borrowRepository.save(borrow);

        List<BorrowDetail> details = borrowDetailRepository.findByBorrowId(borrowId);
        for (BorrowDetail detail : details) {
            detail.setStatus("Borrowed");
            borrowDetailRepository.save(detail);
            
            if (detail.getBookItem() != null) {
                BookItem item = detail.getBookItem();
                item.setStatus("Borrowed");
                bookItemRepository.save(item);
            }
        }
    }

    // UC-13.4: Gia hạn mượn
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processRenewal(Integer borrowDetailId) throws Exception {
        BorrowDetail detail = borrowDetailRepository.findById(borrowDetailId)
                .orElseThrow(() -> new Exception("Không tìm thấy chi tiết phiếu mượn!"));

        if (!"Borrowed".equalsIgnoreCase(detail.getStatus())) {
            throw new Exception("Chỉ sách đang ở trạng thái 'Đang mượn' mới được phép gia hạn!");
        }

        // Đọc cấu hình số lần gia hạn tối đa (mặc định là 2)
        int maxRenewals = 2;
        Optional<SystemSetting> setting = systemSettingRepository.findBySettingKey("MAX_RENEWALS");
        if (setting.isPresent()) {
            try {
                maxRenewals = Integer.parseInt(setting.get().getSettingValue());
            } catch (Exception ignored) {}
        }

        if (detail.getRenewCount() >= maxRenewals) {
            throw new Exception("Sách này đã được gia hạn tối đa " + maxRenewals + " lần!");
        }

        // Đọc cấu hình số ngày gia hạn thêm mỗi lần (mặc định là 7 ngày)
        int renewDays = 7;
        Optional<SystemSetting> renewDaysSetting = systemSettingRepository.findBySettingKey("RENEW_DAYS");
        if (renewDaysSetting.isPresent()) {
            try {
                renewDays = Integer.parseInt(renewDaysSetting.get().getSettingValue());
            } catch (Exception ignored) {}
        }

        detail.setDueDate(detail.getDueDate().plusDays(renewDays));
        detail.setRenewCount(detail.getRenewCount() + 1);
        borrowDetailRepository.save(detail);
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
                    } catch (Exception ignored) {}
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
                                fineAmount.setScale(0).toString(), detail.getBook().getTitle(), overdueDays));
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
            if (!"Returned".equalsIgnoreCase(d.getStatus())) {
                allReturned = false;
            }
            if ("Overdue".equalsIgnoreCase(d.getStatus())) {
                hasOverdue = true;
            }
        }

        if (allReturned) {
            borrow.setStatus("Returned");
        } else if (hasOverdue) {
            borrow.setStatus("Overdue");
        } else {
            borrow.setStatus("Active");
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
            notif.setStatus("Active");
            Notification saved = notificationRepository.save(notif);

            MemberNotification mn = new MemberNotification();
            mn.setId(new MemberNotificationId(member.getMemberId(), saved.getNotificationId()));
            mn.setMember(member);
            mn.setNotification(saved);
            mn.setIsRead(false);
            memberNotificationRepository.save(mn);
        } catch (Exception ignored) {}
    }
}
