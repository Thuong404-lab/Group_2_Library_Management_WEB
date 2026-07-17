package com.lms.service;

import com.lms.entity.Borrow;
import com.lms.entity.BorrowDetail;
import com.lms.entity.Transaction;

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
    // Xác nhận trả sách với tình trạng vật lý tách biệt (bookCondition) và ghi chú hư hỏng (damageNote)
    void confirmReturnWithDetails(String barcode, java.time.LocalDateTime returnDate, String bookCondition, String damageNote, String staffUsername);
    
    // Tìm kiếm các ca mượn đang hoạt động bằng truy vấn đa năng (Barcode, Mã phiếu, SĐT)
    java.util.List<com.lms.entity.BorrowDetail> searchActiveLoansByQuery(String query);
    
    // Xác nhận trả sách hàng loạt với thông tin chi tiết
    void confirmBatchReturnWithDetails(java.util.List<String> barcodes, java.time.LocalDateTime returnDate, String bookCondition, String damageNote, String staffUsername);

    java.util.List<com.lms.entity.BorrowDetail> getTodayReturnedBooks();
    void confirmBookReturn(Integer borrowDetailId, String conditionNote);
    void confirmCollection(Integer borrowId);
    java.util.List<com.lms.entity.BorrowDetail> getBorrowDetailsByBorrowId(Integer borrowId);
    java.util.List<com.lms.entity.Transaction> getTransactionsByBorrowId(Integer borrowId);
    List<BorrowDetail> getBorrowSchedule(String borrowDate, String returnDate, String keyword);
    // =========================================================================
    // PHƯƠNG THỨC MỚI ĐƯỢC ỦY THÁC NGHIỆP VỤ (365 ngày)
    // =========================================================================
    List<BorrowDetail> getMemberBorrowDetailsLimit365Days(Integer memberId);
    List<Transaction> getMemberTransactionsLimit365Days(Integer memberId);
    List<BorrowDetail> getMemberBorrowDetailsByDateRange(Integer memberId, java.time.LocalDateTime startDate, java.time.LocalDateTime endDate);
    List<Transaction> getMemberTransactionsByDateRange(Integer memberId, java.time.LocalDateTime startDate, java.time.LocalDateTime endDate);
}

