package com.lms.service.impl;

import com.lms.service.FinancialService;

import com.lms.repository.TransactionRepository;
import com.lms.repository.WalletRepository;
import org.springframework.stereotype.Service;

import com.lms.entity.SystemSetting;
import com.lms.repository.SystemSettingRepository;

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
    private final SystemSettingRepository systemSettingRepository;

    public FinancialServiceImpl(TransactionRepository transactionRepository,
                                  WalletRepository walletRepository,
                                  com.lms.repository.BorrowRepository borrowRepository,
                                  com.lms.repository.BorrowDetailRepository borrowDetailRepository,
                                  SystemSettingRepository systemSettingRepository) {
        this.transactionRepository = transactionRepository;
        this.walletRepository = walletRepository;
        this.borrowRepository = borrowRepository;
        this.borrowDetailRepository = borrowDetailRepository;
        this.systemSettingRepository = systemSettingRepository;
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

        // UC-8.2: TÍNH PHÍ CỐ ĐỊNH THEO OPTION ĐÃ CÀI
        // Màn hình đang hiển thị theo quy ước: fees = quantity * 10(days) * (borrowFeePerBook).
        int billableDays = 10;

        // Giá tiền mượn lấy từ SystemSettings (ADMIN set). Nếu chưa có, dùng fallback hardcode.
        // Quy ước: borrowFeePerBook là tiền 1 quyển/1 ngày.
        double borrowFeePerBook = 5000d; // fallback
        try {
            java.util.Optional<SystemSetting> settingOpt = systemSettingRepository.findAll().stream()
                    .filter(s -> s.getSettingKey() != null && s.getSettingKey().equalsIgnoreCase("BORROW_FEE_PER_BOOK"))
                    .findFirst();
            if (settingOpt.isPresent() && settingOpt.get().getSettingValue() != null) {
                borrowFeePerBook = Double.parseDouble(settingOpt.get().getSettingValue());
            }
        } catch (Exception ex) {
            borrowFeePerBook = 5000d;
        }

        java.math.BigDecimal feeAmount = java.math.BigDecimal.valueOf(quantity)
                .multiply(java.math.BigDecimal.valueOf(billableDays))
                .multiply(java.math.BigDecimal.valueOf(borrowFeePerBook));

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
