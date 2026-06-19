package com.lms.controller.member;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

/**
 * FinancialController - Giao dịch Tài chính (Phía Member)
 * Người phụ trách: Phạm Kiến Quốc (CE201286)
 */
@Controller
@RequestMapping("/member/financial")
public class FinancialController {

    // UC-8.1: Pay Overdue Fines
    @GetMapping("/fines")
    public String viewOverdueFines(Principal principal, Model model) {
        // TODO: Implement - Lấy danh sách khoản phạt chưa thanh toán
        // TODO: Tính tổng tiền phạt quá hạn
        return "member/fines";
    }

    @PostMapping("/fines/pay/{fineId}")
    public String payOverdueFine(@PathVariable Integer fineId,
                                  Principal principal, Model model) {
        // TODO: Implement - Trừ tiền phạt từ Wallet
        // TODO: Nếu Wallet không đủ → cho phép thẻ âm (theo nghiệp vụ)
        // TODO: Tạo Transaction (type = FINE_PAYMENT)
        return "redirect:/member/financial/fines?paid";
    }

    // UC-8.2: Pay Borrowing Fees
    @GetMapping("/fees")
    public String viewBorrowingFees(Principal principal, Model model) {
        // TODO: Implement - Hiển thị phí mượn sách chưa thanh toán
        return "member/fees";
    }

    // UC-8.3: Pay Reservation Deposit
    @PostMapping("/deposit/{reservationId}")
    public String payReservationDeposit(@PathVariable Integer reservationId,
                                         Principal principal, Model model) {
        // TODO: Implement - Trừ tiền cọc đặt trước từ Wallet
        // TODO: Tạo Transaction (type = RESERVATION_DEPOSIT)
        return "redirect:/member/borrow/reservations?depositPaid";
    }

    // UC-8.4: View Transaction History
    @GetMapping("/transactions")
    public String viewTransactionHistory(Principal principal,
                                          @RequestParam(defaultValue = "0") int page,
                                          Model model) {
        // TODO: Implement - Lấy lịch sử Transaction của Member
        // TODO: Hỗ trợ phân trang và lọc theo loại giao dịch
        return "member/wallet";
    }

    // UC-8.5: View Top-up Notifications
    @GetMapping("/topup-notifications")
    public String viewTopupNotifications(Principal principal, Model model) {
        // TODO: Implement - Lấy thông báo nạp tiền thành công
        return "member/topup-notifications";
    }
}
