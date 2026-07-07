package com.lms.controller.librarian;

import com.lms.service.LoanService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.Arrays;
import java.util.List;

/**
 * LoanController - Quản lý Phiếu mượn (Phía Thủ thư)
 * Người phụ trách: Huỳnh Gia Hưng (CE190488)
 */
@Controller
@RequestMapping("/librarian/loan")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

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
        // TODO: Implement
        return "redirect:/librarian/loan/returns?confirmed";
    }

    // UC-13.3: Process Borrow Requests - Quầy mượn sách
    @GetMapping("/borrow-schedule")
    public String showBorrowSchedule(Model model) {
        model.addAttribute("details", loanService.getAllBorrowDetails());
        return "librarian/borrow-schedule";
    }

    @PostMapping("/borrow-desk/process")
    public String processBorrowRequest(@RequestParam String memberIdentifier,
                                        @RequestParam String barcodes,
                                        Principal principal,
                                        RedirectAttributes redirectAttributes) {
        try {
            List<String> barcodeList = Arrays.asList(barcodes.split(","));
            loanService.processBorrowDesk(memberIdentifier, barcodeList, principal.getName());
            redirectAttributes.addFlashAttribute("success", "Đã tạo phiếu mượn thành công!");
            return "redirect:/librarian/loan/borrow-desk";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/librarian/loan/borrow-desk";
        }
    }

    // UC-13.4: Process Renewal Requests - Gia hạn mượn sách
    @PostMapping("/renew/{borrowDetailId}")
    public String processRenewal(@PathVariable Integer borrowDetailId, RedirectAttributes redirectAttributes) {
        try {
            loanService.processRenewal(borrowDetailId);
            redirectAttributes.addFlashAttribute("success", "Đã gia hạn thành công thêm 7 ngày!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        // Redirect back to the referrer page
        return "redirect:/librarian/loan/borrow-desk"; // You might want to pass the correct redirect URL if needed, but for now this is ok
    }
}
