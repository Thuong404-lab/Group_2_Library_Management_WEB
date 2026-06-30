package com.lms.service;

import com.lms.dto.request.BorrowRequest;
import com.lms.entity.Borrow;

public interface BorrowService {
    // Hàm tạo phiếu trực tiếp tại quầy của thủ thư (Giữ nguyên của nhóm)
    Borrow processBorrowing(BorrowRequest request, String librarianUsername) throws Exception;

    // BỔ SUNG: Độc giả tạo yêu cầu mượn trực tuyến gửi lên (Chờ duyệt)
    Borrow memberSubmitBorrowRequest(String username, Integer bookId, Integer numberOfDays) throws Exception;

    // BỔ SUNG: Nghiệp vụ xác nhận nhận sách hoàn trả tại quầy và giải phóng kho sách
    void processReturnBook(String barcode) throws Exception;
}