package com.lms.controller.librarian;

import com.lms.entity.Borrow;
import com.lms.entity.BorrowDetail;
import com.lms.entity.Transaction;
import com.lms.service.LoanService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.Collections;
import java.util.List;

@Controller
@RequestMapping("/librarian/loan")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    // 1. Mở trang Bàn trả sách
    @GetMapping("/returns")
    public String showReturnDesk() {
        return "librarian/return-desk";
    }

    // 2. Xử lý tìm kiếm (Đã thêm kiểm tra an toàn)
    @GetMapping("/returns/search")
    public String searchActiveReturnLoans(@RequestParam(value = "barcode", required = false) String barcode, Model model) {
        // Kiểm tra an toàn: Nếu barcode là null hoặc rỗng thì không tìm kiếm
        if (barcode == null || barcode.trim().isEmpty()) {
            model.addAttribute("searchResults", Collections.emptyList());
            return "librarian/return-desk";
        }

        String trimmedBarcode = barcode.trim();
        List<BorrowDetail> searchResults = loanService.findActiveLoansByBarcode(trimmedBarcode);

        model.addAttribute("searchResults", searchResults);
        model.addAttribute("barcode", trimmedBarcode); // Lưu lại barcode vào Model để hiển thị trong ô input

        if (searchResults.isEmpty()) {
            model.addAttribute("errorMessage", "Không tìm thấy lượt mượn nào với mã vạch: " + trimmedBarcode);
        }
        return "librarian/return-desk";
    }

    // 3. Xác nhận trả sách
    @PostMapping("/returns/confirm/{id}")
    public String confirmReturnBook(@PathVariable("id") Integer borrowDetailId,
                                    @RequestParam("conditionNote") String conditionNote,
                                    RedirectAttributes redirectAttributes) {
        try {
            loanService.confirmBookReturn(borrowDetailId, conditionNote);
            redirectAttributes.addFlashAttribute("successMessage", "Xác nhận nhận trả sách thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/librarian/loan/returns";
    }

    @GetMapping("/borrow-schedule")
    public String viewBorrowSchedule(
            @RequestParam(required = false) String borrowDate,
            @RequestParam(required = false) String returnDate,
            @RequestParam(required = false) String keyword,
            Model model) {

        // Gọi service để lấy danh sách chi tiết (Bạn cần đảm bảo hàm này tồn tại trong LoanService)
        List<BorrowDetail> details = loanService.getBorrowSchedule(borrowDate, returnDate, keyword);
        model.addAttribute("details", details);

        return "librarian/borrow-schedule"; // Tên file .html của bạn
    }

    // 4. Xem chi tiết phiếu mượn
    @GetMapping("/{id}")
    public String showLoanDetail(@PathVariable("id") Integer borrowId, Model model) {
        try {
            Borrow borrow = loanService.getLoanDetails(borrowId);
            model.addAttribute("borrow", borrow);
            model.addAttribute("details", loanService.getBorrowDetailsByBorrowId(borrowId));
            model.addAttribute("transactions", loanService.getTransactionsByBorrowId(borrowId));
            return "librarian/loan-detail";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Không thể hiển thị chi tiết: " + e.getMessage());
            return "redirect:/librarian/borrow/list";
        }
    }
    // Đảm bảo URL này khớp với th:href trong file HTML của bạn

}