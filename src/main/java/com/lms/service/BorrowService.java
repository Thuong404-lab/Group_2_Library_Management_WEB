package com.lms.service;

import org.springframework.stereotype.Service;

/**
 * BorrowService - Xử lý Logic Mượn/Trả sách
 * Người phụ trách: Huỳnh Gia Hưng (CE190488)
 */
@Service
public class BorrowService {

    // UC-6.1: Lấy lịch sử mượn
    public void getBorrowHistory(Integer memberId, int page) {
        // TODO: Implement - Lấy danh sách Borrow + BorrowDetails
    }

    // UC-6.2: Đặt trước sách
    public void reserveBook(Integer memberId, Integer bookId) {
        // TODO: Implement - Tạo Reservation
        // TODO: Trừ tiền cọc từ Wallet
    }

    // UC-6.3: Mượn sách (Thủ thư xử lý)
    public void processBorrow(String memberPhone, String[] barcodes) {
        // TODO: Implement - Thuật toán 7 bước:
        // 1. Tìm Member qua SĐT
        // 2. Check từng barcode sách
        // 3. Tính tổng phí mượn
        // 4. Kiểm tra Wallet đủ tiền
        // 5. Trừ tiền Wallet → Tạo Transaction
        // 6. Tạo Borrow + BorrowDetails
        // 7. Cập nhật trạng thái BookItem = "Borrowed"
    }

    // UC-6.4: Trả sách
    public void processReturn(Integer borrowDetailId) {
        // TODO: Implement
        // TODO: Kiểm tra quá hạn → tính phí phạt
        // TODO: Cập nhật BookItem = "Available"
    }
}
