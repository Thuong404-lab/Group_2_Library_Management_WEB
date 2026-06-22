package com.lms.service;

/**
 * FinancialService - Xử lý Logic Tài chính & Phạt
 * Người phụ trách: Phạm Kiến Quốc (CE201286)
 */
public interface FinancialService {

    // UC-8.1: Thanh toán phí phạt quá hạn
    void payOverdueFine(Integer memberId, Integer fineId);

    // UC-8.2: Thanh toán phí mượn
    void payBorrowingFee(Integer memberId, Double amount);

    // UC-8.3: Thanh toán tiền cọc đặt trước
    void payReservationDeposit(Integer memberId, Integer reservationId);

    // UC-8.4: Lấy lịch sử giao dịch (Member)
    void getTransactionHistory(Integer memberId, int page);

    // UC-14.2: Tạo phạt vi phạm (Thủ thư)
    void createFine(Integer memberId, Double amount, String reason);

    // UC-14.3: Lấy lịch sử giao dịch toàn hệ thống (Thủ thư)
    void getAllTransactions(int page, String type);

    // UC-14.4: Nạp tiền vào Wallet (Thủ thư tại quầy)
    void topUpMemberAccount(String memberPhone, Double amount);

}
