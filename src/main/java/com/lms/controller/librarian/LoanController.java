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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

@Controller
@RequestMapping("/librarian/loan")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    // 1. Mở trang Bàn trả sách (Đã bổ sung các thuộc tính mặc định)
    @GetMapping("/returns")
    public String showReturnDesk(Model model) {
        model.addAttribute("defaultReturnDate", LocalDate.now());
        model.addAttribute("searchedBarcode", "");
        return "librarian/return-desk";
    }

    // 2. Xử lý tìm kiếm (Đã thêm các thuộc tính model còn thiếu)
    @GetMapping("/returns/search")
    public String searchActiveReturnLoans(@RequestParam(value = "barcode", required = false) String barcode, Model model) {
        model.addAttribute("defaultReturnDate", LocalDate.now());

        // Kiểm tra an toàn: Nếu barcode là null hoặc rỗng thì không tìm kiếm
        if (barcode == null || barcode.trim().isEmpty()) {
            model.addAttribute("searchResults", Collections.emptyList());
            model.addAttribute("searchedBarcode", "");
            return "librarian/return-desk";
        }

        String trimmedBarcode = barcode.trim();
        List<BorrowDetail> searchResults = loanService.findActiveLoansByBarcode(trimmedBarcode);

        model.addAttribute("searchResults", searchResults);
        model.addAttribute("barcode", trimmedBarcode);
        model.addAttribute("searchedBarcode", trimmedBarcode); // Đồng bộ thuộc tính searchedBarcode cho view

        if (searchResults.isEmpty()) {
            model.addAttribute("errorMessage", "Không tìm thấy lượt mượn nào với mã vạch: " + trimmedBarcode);
        }
        return "librarian/return-desk";
    }

    // 3. Xác nhận trả sách theo từng ID cụ thể (Giữ nguyên tương thích cũ)
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

    // [BỔ SUNG MỚI] POST endpoint nhận dữ liệu từ form Modal tại return-desk.html (Không chứa ID trên Path)
    @PostMapping("/returns/confirm")
    public String confirmReturnBookFromModal(@RequestParam("barcode") String barcode,
                                             @RequestParam("returnDate") String returnDateStr,
                                             @RequestParam("conditionNote") String conditionNote,
                                             @RequestParam(value = "conditionNoteAdditional", required = false) String conditionNoteAdditional,
                                             Principal principal,
                                             RedirectAttributes redirectAttributes) {
        try {
            // 1. Ghép chuỗi tình trạng ngoại quan chính với ghi chú bổ sung nếu có
            String finalConditionNote = conditionNote;
            if (conditionNoteAdditional != null && !conditionNoteAdditional.trim().isEmpty()) {
                finalConditionNote = conditionNote + " - " + conditionNoteAdditional.trim();
            }

            // 2. Chuyển đổi định dạng ngày nhận được từ form (yyyy-MM-dd) thành LocalDateTime
            LocalDate parsedDate = LocalDate.parse(returnDateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            LocalDateTime actualReturnDateTime = LocalDateTime.of(parsedDate, LocalTime.now());

            // 3. Lấy thông tin username người thực hiện giao dịch
            String staffUsername = (principal != null) ? principal.getName() : "admin";

            // 4. Thực thi nghiệp vụ tiếp nhận và trả sách
            loanService.confirmReturnWithDetails(barcode.trim(), actualReturnDateTime, finalConditionNote, staffUsername);

            redirectAttributes.addFlashAttribute("successMessage", "Xác nhận nhận trả sách thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi tiếp nhận trả sách: " + e.getMessage());
        }
        return "redirect:/librarian/loan/returns";
    }

    @GetMapping("/borrow-schedule")
    public String viewBorrowSchedule(
            @RequestParam(required = false) String borrowDate,
            @RequestParam(required = false) String returnDate,
            @RequestParam(required = false) String keyword,
            Model model) {

        List<BorrowDetail> details = loanService.getBorrowSchedule(borrowDate, returnDate, keyword);
        model.addAttribute("details", details);

        return "librarian/borrow-schedule";
    }

    // [CẢI TIẾN MỚI] Giải quyết lỗi Static Resource cho endpoint "detail-preview"
    // Hỗ trợ tiếp nhận cả tham số "?id=" hoặc "?borrowId=" để chuyển hướng mượt mà
    @GetMapping("/borrow-schedule/detail-preview")
    public String previewLoanDetail(
            @RequestParam(value = "id", required = false) Integer id,
            @RequestParam(value = "borrowId", required = false) Integer borrowId,
            RedirectAttributes redirectAttributes) {

        Integer targetId = (id != null) ? id : borrowId;

        if (targetId == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy ID của phiếu mượn yêu cầu preview.");
            return "redirect:/librarian/loan/borrow-schedule";
        }

        // Chuyển tiếp (Redirect) yêu cầu sang cấu trúc xem chi tiết chuẩn theo ID
        return "redirect:/librarian/loan/" + targetId;
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
}