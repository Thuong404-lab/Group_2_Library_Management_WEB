package com.lms.service.impl;

import com.lms.service.BorrowService;

import org.springframework.stereotype.Service;

/**
 * BorrowService - Xử lý Logic Mượn/Trả sách
 * Người phụ trách: Huỳnh Gia Hưng (CE190488)
 */
@Service
public class BorrowServiceImpl implements BorrowService {

    // UC-6.1: Lấy lịch sử mượn
    @Override
    public void getBorrowHistory(Integer memberId, int page) {
        // TODO: Implement - Lấy danh sách Borrow + BorrowDetails
    }

    // UC-6.2: Đặt trước sách
    @Override
    public void reserveBook(Integer memberId, Integer bookId) {
        // TODO: Implement - Tạo Reservation
        // TODO: Trừ tiền cọc từ Wallet
    }

    // UC-6.3: Mượn sách (Thủ thư xử lý)
    @Override
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
    @Override
    public void processReturn(Integer borrowDetailId) {
        // TODO: Implement
        // TODO: Kiểm tra quá hạn → tính phí phạt
        // TODO: Cập nhật BookItem = "Available"
    }
}
