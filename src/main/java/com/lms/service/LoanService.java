package com.lms.service;

import com.lms.entity.Borrow;

/**
 * LoanService - Xử lý Logic Quản lý Phiếu mượn (Thủ thư)
 * Người phụ trách: Huỳnh Gia Hưng (CE190488)
 */
public interface LoanService {

    // UC-13.1: Xem chi tiết phiếu mượn
    Borrow getLoanDetails(Integer borrowId) throws Exception;

    // UC-13.2: Xác nhận trả sách vật lý qua quét barcode
    void confirmReturn(String barcode) throws Exception;

    // Phê duyệt yêu cầu trả sách trực tuyến từ độc giả
    void approveOnlineReturn(Integer borrowId) throws Exception;

    // UC-13.3: Duyệt yêu cầu mượn
    void processBorrowRequest(Integer borrowId) throws Exception;

    // UC-13.4: Gia hạn mượn
    void processRenewal(Integer borrowDetailId) throws Exception;

}
