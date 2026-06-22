package com.lms.service;

/**
 * LoanService - Xử lý Logic Quản lý Phiếu mượn (Thủ thư)
 * Người phụ trách: Huỳnh Gia Hưng (CE190488)
 */
public interface LoanService {

    // UC-13.1: Xem chi tiết phiếu mượn
    void getLoanDetails(Integer borrowId);

    // UC-13.2: Xác nhận trả sách
    void confirmReturn(String barcode, Integer memberId);

    // UC-13.3: Duyệt yêu cầu mượn
    void processBorrowRequest(Integer borrowId);

    // UC-13.4: Gia hạn mượn
    void processRenewal(Integer borrowDetailId);

}
