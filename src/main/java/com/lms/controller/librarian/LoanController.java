package com.lms.controller.librarian;

import com.lms.entity.Borrow;
import com.lms.entity.BorrowDetail;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/librarian/loan")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    // 1. Mở trang Bàn trả sách
    @GetMapping("/returns")
    public String showReturnDesk(Model model) {
        model.addAttribute("defaultReturnDate", LocalDate.now());
        model.addAttribute("searchedBarcode", "");
        return "librarian/return-desk";
    }

    // 2. Xử lý tìm kiếm sách trả đa năng (Barcode, Mã phiếu, SĐT)
    @GetMapping("/returns/search")
    public String searchActiveReturnLoans(@RequestParam(value = "query", required = false) String query, Model model) {
        model.addAttribute("defaultReturnDate", LocalDate.now());

        if (query == null || query.trim().isEmpty()) {
            model.addAttribute("groupedResults", Collections.emptyMap());
            model.addAttribute("searchedQuery", "");
            return "librarian/return-desk";
        }

        String trimmedQuery = query.trim();
        List<BorrowDetail> searchResults = loanService.searchActiveLoansByQuery(trimmedQuery);

        // Gom nhóm chi tiết mượn hoạt động theo từng phiếu mượn (đơn cha)
        Map<Borrow, List<BorrowDetail>> groupedResults = new LinkedHashMap<>();
        for (BorrowDetail detail : searchResults) {
            Borrow b = detail.getBorrow();
            if (b != null) {
                groupedResults.computeIfAbsent(b, k -> new ArrayList<>()).add(detail);
            }
        }

        model.addAttribute("groupedResults", groupedResults);
        model.addAttribute("query", trimmedQuery);
        model.addAttribute("searchedQuery", trimmedQuery);

        if (groupedResults.isEmpty()) {
            model.addAttribute("errorMessage", "Không tìm thấy phiếu mượn đang hoạt động nào ứng với từ khóa: " + trimmedQuery);
        }
        return "librarian/return-desk";
    }

    // 3. Xác nhận trả sách theo từng ID cụ thể
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

    // 4. POST endpoint nhận dữ liệu từ form Modal tại return-desk.html (Đơn lẻ)
    @PostMapping("/returns/confirm")
    public String confirmReturnBookFromModal(@RequestParam(value = "barcode", required = false) String barcode,
                                             @RequestParam("returnDate") String returnDateStr,
                                             @RequestParam("conditionNote") String conditionNote,
                                             @RequestParam(value = "conditionNoteAdditional", required = false) String conditionNoteAdditional,
                                             Principal principal,
                                             RedirectAttributes redirectAttributes) {
        if (barcode == null || barcode.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi tiếp nhận: Chưa nhận dạng được mã Barcode sách hoàn trả!");
            return "redirect:/librarian/loan/returns";
        }
        try {
            LocalDate parsedDate = LocalDate.parse(returnDateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            LocalDateTime actualReturnDateTime = LocalDateTime.of(parsedDate, LocalTime.now());

            String staffUsername = (principal != null) ? principal.getName() : "admin";

            loanService.confirmReturnWithDetails(barcode.trim(), actualReturnDateTime, conditionNote, conditionNoteAdditional, staffUsername);

            redirectAttributes.addFlashAttribute("successMessage", "Xác nhận nhận trả sách thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi tiếp nhận trả sách: " + e.getMessage());
        }
        return "redirect:/librarian/loan/returns";
    }

    // 4b. POST endpoint nhận dữ liệu trả hàng loạt (Batch Return)
    @PostMapping("/returns/confirm-batch")
    public String confirmBatchReturnBookFromModal(@RequestParam(value = "barcodes", required = false) List<String> barcodes,
                                                  @RequestParam("returnDate") String returnDateStr,
                                                  @RequestParam("conditionNote") String conditionNote,
                                                  @RequestParam(value = "conditionNoteAdditional", required = false) String conditionNoteAdditional,
                                                  Principal principal,
                                                  RedirectAttributes redirectAttributes) {
        if (barcodes == null || barcodes.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi tiếp nhận: Danh sách mã Barcode sách hoàn trả trống!");
            return "redirect:/librarian/loan/returns";
        }
        try {
            LocalDate parsedDate = LocalDate.parse(returnDateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            LocalDateTime actualReturnDateTime = LocalDateTime.of(parsedDate, LocalTime.now());

            String staffUsername = (principal != null) ? principal.getName() : "admin";

            loanService.confirmBatchReturnWithDetails(barcodes, actualReturnDateTime, conditionNote, conditionNoteAdditional, staffUsername);

            redirectAttributes.addFlashAttribute("successMessage", "Xác nhận hoàn trả sách hàng loạt thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi tiếp nhận trả sách hàng loạt: " + e.getMessage());
        }
        return "redirect:/librarian/loan/returns";
    }

    // 5. Xem chi tiết phiếu mượn
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
            return "redirect:/librarian/borrow/member-history";
        }
    }
}