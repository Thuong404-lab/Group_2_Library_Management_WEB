package com.lms.service;

import com.lms.entity.Borrow;
import com.lms.entity.BorrowDetail;
import java.util.List;

/**
 * LoanService - Xử lý Logic Quản lý Phiếu mượn (Thủ thư)
 * Người phụ trách: Huỳnh Gia Hưng (CE190488)
 */
public interface LoanService {

    // UC-13.1: Xem chi tiết phiếu mượn
    Borrow getLoanDetails(Integer borrowId) ;

    // UC-13.2: Xác nhận trả sách vật lý qua quét barcode
    void confirmReturn(String barcode) ;

    // Phê duyệt yêu cầu trả sách trực tuyến từ độc giả
    void approveOnlineReturn(Integer borrowId) ;

    // UC-13.3: Quầy mượn sách
    void processBorrowDesk(String memberIdentifier, List<String> barcodes, String staffUsername) ;

    // UC-13.3: Duyệt yêu cầu mượn
    void processBorrowRequest(Integer borrowId) ;

    // UC-13.4: Gia hạn mượn
    void processRenewal(Integer borrowDetailId) ;
    void approveRenewal(Integer borrowDetailId, String staffUsername);
    void rejectRenewal(Integer borrowDetailId, String staffUsername);
    List<BorrowDetail> getAllPendingRenewals();

    List<BorrowDetail> getAllBorrowDetails();

    java.util.List<com.lms.entity.BorrowDetail> findActiveLoansByBarcode(String barcode);
    void confirmReturnWithDetails(String barcode, java.time.LocalDateTime returnDate, String conditionNote, String staffUsername);
    java.util.List<com.lms.entity.BorrowDetail> getTodayReturnedBooks();
    void confirmBookReturn(Integer borrowDetailId, String conditionNote);
    java.util.List<com.lms.entity.BorrowDetail> getBorrowDetailsByBorrowId(Integer borrowId);
    java.util.List<com.lms.entity.Transaction> getTransactionsByBorrowId(Integer borrowId);
    List<BorrowDetail> getBorrowSchedule(String borrowDate, String returnDate, String keyword);
}
