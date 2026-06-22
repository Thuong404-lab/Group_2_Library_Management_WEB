package com.lms.service;

import org.springframework.stereotype.Service;

/**
 * FinancialService - Xử lý Logic Tài chính & Phạt
 * Người phụ trách: Phạm Kiến Quốc (CE201286)
 */
@Service
public class FinancialService {

    // UC-8.1: Thanh toán phí phạt quá hạn
    public void payOverdueFine(Integer memberId, Integer fineId) {
        // TODO: Implement - Trừ tiền từ Wallet
        // TODO: Cho phép Wallet âm theo nghiệp vụ
        // TODO: Tạo Transaction (type = FINE_PAYMENT)
    }

    // UC-8.2: Thanh toán phí mượn
    public void payBorrowingFee(Integer memberId, Double amount) {
        // TODO: Implement
    }

    // UC-8.3: Thanh toán tiền cọc đặt trước
    public void payReservationDeposit(Integer memberId, Integer reservationId) {
        // TODO: Implement
    }

    // UC-8.4: Lấy lịch sử giao dịch (Member)
    public void getTransactionHistory(Integer memberId, int page) {
        // TODO: Implement
    }

    // UC-14.2: Tạo phạt vi phạm (Thủ thư)
    public void createFine(Integer memberId, Double amount, String reason) {
        // TODO: Implement
    }

    // UC-14.3: Lấy lịch sử giao dịch toàn hệ thống (Thủ thư)
    public void getAllTransactions(int page, String type) {
        // TODO: Implement
    }

    // UC-14.4: Nạp tiền vào Wallet (Thủ thư tại quầy)
    public void topUpMemberAccount(String memberPhone, Double amount) {
        // TODO: Implement - Cộng tiền vào Wallet
        // TODO: Tạo Transaction (type = TOP_UP)
        // TODO: Gửi Notification xác nhận
    }
}
