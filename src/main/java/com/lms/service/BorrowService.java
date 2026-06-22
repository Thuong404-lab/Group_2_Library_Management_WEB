package com.lms.service;

/**
 * BorrowService - Xử lý Logic Mượn/Trả sách
 * Người phụ trách: Huỳnh Gia Hưng (CE190488)
 */
public interface BorrowService {

    // UC-6.1: Lấy lịch sử mượn
    void getBorrowHistory(Integer memberId, int page);

    // UC-6.2: Đặt trước sách
    void reserveBook(Integer memberId, Integer bookId);

    // UC-6.3: Mượn sách (Thủ thư xử lý)
    void processBorrow(String memberPhone, String[] barcodes);

    // UC-6.4: Trả sách
    void processReturn(Integer borrowDetailId);

}
