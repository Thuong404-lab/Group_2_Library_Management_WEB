package com.lms.controller.member;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

/**
 * BorrowController - Quản lý Mượn/Trả sách (Phía Member)
 * Người phụ trách: Huỳnh Gia Hưng (CE190488)
 */
@Controller
@RequestMapping("/member/borrow")
public class BorrowController {

    // UC-6.1: View Borrowing History
    @GetMapping("/history")
    public String viewBorrowingHistory(Principal principal,
                                        @RequestParam(defaultValue = "0") int page,
                                        Model model) {
        // TODO: Implement - Lấy danh sách phiếu mượn của Member
        // TODO: Hỗ trợ phân trang, lọc theo trạng thái (Borrowed, Returned, Overdue)
        return "member/borrow-history";
    }

    // UC-6.2: Reserve Books - Đặt trước sách
    @PostMapping("/reserve/{bookId}")
    public String reserveBook(@PathVariable Integer bookId,
                               Principal principal, Model model) {
        // TODO: Implement - Kiểm tra sách còn available không
        // TODO: Kiểm tra Member đã đặt trước quá giới hạn chưa
        // TODO: Tạo Reservation mới, trừ tiền cọc từ Wallet
        return "redirect:/books/" + bookId + "?reserved";
    }

    // UC-6.2: View Reservations
    @GetMapping("/reservations")
    public String viewReservations(Principal principal, Model model) {
        // TODO: Implement - Lấy danh sách Reservation của Member
        return "member/reservations";
    }

    // UC-6.3: Borrow Book (Member xem trạng thái mượn)
    @GetMapping("/current")
    public String viewCurrentBorrows(Principal principal, Model model) {
        // TODO: Implement - Lấy các sách đang mượn chưa trả
        // TODO: Hiển thị ngày hết hạn, cảnh báo sắp quá hạn
        return "member/current-borrows";
    }

    // UC-6.4: Return Books (Member xem sách cần trả)
    @GetMapping("/returns")
    public String viewPendingReturns(Principal principal, Model model) {
        // TODO: Implement - Lấy danh sách sách cần trả
        // TODO: Tính toán phí phạt nếu quá hạn
        return "member/pending-returns";
    }
}
