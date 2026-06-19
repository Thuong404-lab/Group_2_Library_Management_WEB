package com.lms.controller.librarian;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * LoanController - Quản lý Phiếu mượn (Phía Thủ thư)
 * Người phụ trách: Huỳnh Gia Hưng (CE190488)
 */
@Controller
@RequestMapping("/librarian/loan")
public class LoanController {

    // UC-13.1: View Loan Details - Xem chi tiết phiếu mượn
    @GetMapping("/{borrowId}")
    public String viewLoanDetails(@PathVariable Integer borrowId, Model model) {
        // TODO: Implement - Lấy Borrow + BorrowDetails + Member info
        return "librarian/loan-detail";
    }

    // UC-13.2: Confirm Book Returns - Xác nhận trả sách
    @GetMapping("/returns")
    public String showReturnDesk(Model model) {
        // TODO: Implement - Hiển thị quầy trả sách
        return "librarian/return-desk";
    }

    @PostMapping("/returns/confirm")
    public String confirmBookReturn(@RequestParam String barcode,
                                     @RequestParam Integer memberId,
                                     Model model) {
        // TODO: Implement - Quét barcode sách
        // TODO: Tìm BorrowDetail đang mượn của Member
        // TODO: Tính phí phạt nếu quá hạn → trừ Wallet
        // TODO: Cập nhật trạng thái BorrowDetail = "Returned"
        // TODO: Cập nhật trạng thái BookItem = "Available"
        return "redirect:/librarian/loan/returns?confirmed";
    }

    // UC-13.3: Process Borrow Requests - Quầy mượn sách
    @GetMapping("/borrow-desk")
    public String showBorrowDesk(Model model) {
        // TODO: Implement - Hiển thị quầy mượn sách cho Thủ thư
        return "librarian/borrow-desk";
    }

    @PostMapping("/borrow-desk/process")
    public String processBorrowRequest(@RequestParam String memberPhone,
                                        @RequestParam String barcodes,
                                        Model model) {
        // TODO: Implement - Tìm Member theo SĐT/ID
        // TODO: Quét barcode từng quyển sách
        // TODO: Kiểm tra Wallet đủ tiền phí mượn
        // TODO: Trừ tiền → Tạo Borrow + BorrowDetails → Cập nhật BookItem
        return "redirect:/librarian/loan/borrow-desk?processed";
    }

    // UC-13.4: Process Renewal Requests - Gia hạn mượn sách
    @PostMapping("/renew/{borrowDetailId}")
    public String processRenewal(@PathVariable Integer borrowDetailId, Model model) {
        // TODO: Implement - Kiểm tra số lần gia hạn (max 2 lần)
        // TODO: Gia hạn thêm 7 ngày
        // TODO: Trừ phí gia hạn từ Wallet (nếu có)
        return "redirect:/librarian/loan/borrow-desk?renewed";
    }
}
