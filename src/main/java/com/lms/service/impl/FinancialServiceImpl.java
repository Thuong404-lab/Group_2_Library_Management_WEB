package com.lms.service.impl;

import com.lms.service.FinancialService;

import com.lms.repository.TransactionRepository;
import com.lms.repository.WalletRepository;
import org.springframework.stereotype.Service;

/**
 * FinancialService - Xử lý Logic Tài chính & Phạt
 * Người phụ trách: Phạm Kiến Quốc (CE201286)
 */
@Service
public class FinancialServiceImpl implements FinancialService {
    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final com.lms.repository.BorrowRepository borrowRepository;
    private final com.lms.repository.BorrowDetailRepository borrowDetailRepository;

    public FinancialServiceImpl(TransactionRepository transactionRepository,
                                  WalletRepository walletRepository,
                                  com.lms.repository.BorrowRepository borrowRepository,
                                  com.lms.repository.BorrowDetailRepository borrowDetailRepository) {
        this.transactionRepository = transactionRepository;
        this.walletRepository = walletRepository;
        this.borrowRepository = borrowRepository;
        this.borrowDetailRepository = borrowDetailRepository;
    }


    // UC-8.1: Thanh toán phí phạt quá hạn
    @Override
    public void payOverdueFine(Integer memberId, Integer fineId) {
        // TODO: Implement - Trừ tiền từ Wallet
        // TODO: Cho phép Wallet âm theo nghiệp vụ
        // TODO: Tạo Transaction (type = FINE_PAYMENT)
    }

    // UC-8.2: Thanh toán phí mượn theo phiếu mượn (BorrowDetail)
    @Override
    public void payBorrowingFee(Integer memberId, Integer borrowId) {
        var borrow = borrowRepository.findById(borrowId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiếu mượn với ID: " + borrowId));

        if (borrow.getMember() == null || borrow.getMember().getMemberId() == null || !borrow.getMember().getMemberId().equals(memberId)) {
            throw new RuntimeException("Phiếu mượn không thuộc về thành viên hiện tại.");
        }

        var details = borrowDetailRepository.findByBorrowId(borrowId);
        if (details == null || details.isEmpty()) {
            throw new RuntimeException("Phiếu mượn không có chi tiết.");
        }

        int quantity = details.size();

        // Tính theo "gói" ngày mượn dựa vào dueDate của chi tiết (lấy trung bình để suy ra gói).
        // Giá cơ bản: 5.000đ/ngày.
        // - 7 ngày: 5.000đ/ngày
        // - 14 ngày: giảm 10% còn 4.500đ/ngày
        // - 30 ngày: giảm 20% còn 4.000đ/ngày
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.Duration avgDuration = details.stream()
                .filter(d -> d.getDueDate() != null)
                .map(d -> d.getDueDate().toLocalDate().atStartOfDay())
                .map(d -> java.time.Duration.between(now, d))
                .reduce(java.time.Duration.ZERO, java.time.Duration::plus);

        int avgDays = 10;
        if (!details.isEmpty()) {
            long totalMillis = avgDuration.toMillis();
            avgDays = (int) Math.round((double) totalMillis / (24.0 * 60 * 60 * 1000));
        }

        int perDayFee;
        if (avgDays <= 9) {
            perDayFee = 5000; // mặc định gói 7 ngày
        } else if (avgDays <= 20) {
            perDayFee = 4500; // gói 14 ngày
        } else {
            perDayFee = 4000; // gói 30 ngày
        }

        int billableDays;
        if (avgDays <= 9) billableDays = 7;
        else if (avgDays <= 20) billableDays = 14;
        else billableDays = 30;

        java.math.BigDecimal feeAmount = java.math.BigDecimal.valueOf(quantity)
                .multiply(java.math.BigDecimal.valueOf(billableDays))
                .multiply(java.math.BigDecimal.valueOf(perDayFee));

        var wallet = walletRepository.findByMemberMemberId(memberId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ví của thành viên."));

        java.math.BigDecimal currentBalance = wallet.getBalance() == null ? java.math.BigDecimal.ZERO : wallet.getBalance();
        wallet.setBalance(currentBalance.subtract(feeAmount));
        walletRepository.save(wallet);

        com.lms.entity.Transaction transaction = new com.lms.entity.Transaction();
        transaction.setWallet(wallet);
        transaction.setBorrow(borrow.getBorrowId() != null ? borrow : null);
        transaction.setTransactionType("BORROW_FEE");
        transaction.setAmount(feeAmount.negate());
        transaction.setTransactionDate(java.time.LocalDateTime.now());
        transaction.setStatus("Completed");
        transactionRepository.save(transaction);
    }

    // UC-8.3: Thanh toán tiền cọc đặt trước
    @Override
    public void payReservationDeposit(Integer memberId, Integer reservationId) {
        // TODO: Implement
    }

    // UC-8.4: Lấy lịch sử giao dịch (Member)
    @Override
    public void getTransactionHistory(Integer memberId, int page) {
        // TODO: Implement
    }

    // UC-14.2: Tạo phạt vi phạm (Thủ thư)
    @Override
    public void createFine(Integer memberId, Double amount, String reason) {
        // TODO: Implement
    }

    // UC-14.3: Lấy lịch sử giao dịch toàn hệ thống (Thủ thư)
    @Override
    public void getAllTransactions(int page, String type) {
        // TODO: Implement
    }

    // UC-14.4: Nạp tiền vào Wallet (Thủ thư tại quầy)
    @Override
    public void topUpMemberAccount(String memberPhone, Double amount) {
        // TODO: Implement - Cộng tiền vào Wallet
        // TODO: Tạo Transaction (type = TOP_UP)
        // TODO: Gửi Notification xác nhận
    }
}
