package com.lms.service;

import com.lms.entity.BorrowDetail;
import java.util.List;

/**
 * LoanService - Xử lý Logic Quản lý Phiếu mượn (Thủ thư)
 * Người phụ trách: Huỳnh Gia Hưng (CE190488)
 */
public interface LoanService {

    // UC-13.1: Xem chi tiết phiếu mượn
    void getLoanDetails(Integer borrowId);

    // UC-13.2: Xác nhận trả sách
    void confirmReturn(String barcode, Integer memberId);

    // UC-13.3: Quầy mượn sách
    void processBorrowDesk(String memberIdentifier, List<String> barcodes, String staffUsername) throws Exception;

    // UC-13.4: Gia hạn mượn
    void processRenewal(Integer borrowDetailId) throws Exception;

    List<BorrowDetail> getAllBorrowDetails();
}
