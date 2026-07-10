package com.lms.controller.librarian;

import com.lms.entity.Borrow;
import com.lms.repository.TransactionRepository;
import com.lms.service.BorrowService;
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
    private final BorrowService borrowService;
    private final TransactionRepository transactionRepository;

    public LoanController(LoanService loanService, BorrowService borrowService, TransactionRepository transactionRepository) {
        this.loanService = loanService;
        this.borrowService = borrowService;
        this.transactionRepository = transactionRepository;
    }

    // UC-13.1: View Loan Details - Xem chi tiết phiếu mượn
    @GetMapping("/{borrowId}")
    public String viewLoanDetails(@PathVariable Integer borrowId, Model model) {
        try {
            Borrow borrow = loanService.getLoanDetails(borrowId);
            model.addAttribute("borrow", borrow);
            model.addAttribute("details", borrowService.getBorrowDetailsByBorrowId(borrowId));
            model.addAttribute("transactions", transactionRepository.findByBorrow_BorrowId(borrowId));
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Không thể lấy thông tin chi tiết phiếu mượn: " + e.getMessage());
            return "error/500";
        }
        return "librarian/loan-detail";
    }

    // UC-13.2: Confirm Book Returns - Hiển thị quầy trả sách
    @GetMapping("/returns")
    public String showReturnDesk(Model model) {
        model.addAttribute("returnRequests", borrowService.getAllReturnRequests());
        return "librarian/return-desk";
    }

    // Xác nhận trả sách trực tiếp tại quầy qua mã vạch
    @PostMapping("/returns/scan")
    public String confirmBookReturn(@RequestParam String barcode, RedirectAttributes redirectAttributes) {
        try {
            loanService.confirmReturn(barcode);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xác nhận trả sách mã vạch '" + barcode + "' thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Trả sách thất bại: " + e.getMessage());
        }
        return "redirect:/librarian/loan/returns";
    }

    // Xác nhận phê duyệt yêu cầu trả sách online
    @PostMapping("/returns/approve/{borrowId}")
    public String approveOnlineReturn(@PathVariable Integer borrowId, RedirectAttributes redirectAttributes) {
        try {
            loanService.approveOnlineReturn(borrowId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã phê duyệt yêu cầu trả sách online cho phiếu mượn #" + borrowId + " thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Phê duyệt trả sách thất bại: " + e.getMessage());
        }
        return "redirect:/librarian/loan/returns";
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
            redirectAttributes.addFlashAttribute("successMessage", "Đã tạo phiếu mượn thành công!");
            return "redirect:/librarian/borrow/create";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/librarian/borrow/create";
        }
    }

    // UC-13.4: Process Renewal Requests - Gia hạn mượn sách
    @PostMapping("/renew/{borrowDetailId}")
    public String processRenewal(@PathVariable Integer borrowDetailId, RedirectAttributes redirectAttributes) {
        try {
            loanService.processRenewal(borrowDetailId);
            redirectAttributes.addFlashAttribute("successMessage", "Gia hạn sách thành công thêm 7 ngày!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Gia hạn thất bại: " + e.getMessage());
        }
        return "redirect:/librarian/loan/borrow-schedule";
    }
}
