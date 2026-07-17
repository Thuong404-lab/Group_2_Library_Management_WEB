package com.lms.controller.librarian;
import com.lms.exception.ApplicationException;
import com.lms.exception.ValidationException;
import com.lms.controller.LocalizedControllerSupport;

import com.lms.entity.BorrowDetail;
import com.lms.service.LoanService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/librarian/loan")
public class LoanController extends LocalizedControllerSupport {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    /**
     * GET: /librarian/loan/returns
     * Màn hình mặc định của Bàn Trả Sách - Tự động nạp danh sách sách đã được trả thành công hôm nay.
     */
    @GetMapping("/returns")
    public String showReturnDesk(Model model) {
        model.addAttribute("todayReturned", loanService.getTodayReturnedBooks());
        model.addAttribute("defaultReturnDate", LocalDate.now());
        return "librarian/return-desk";
    }

    /**
     * GET: /librarian/loan/returns/search
     * Xử lý tìm kiếm lượt mượn hoạt động (Borrowed/Overdue/Return_Pending) của mã Barcode vừa quét.
     */
    @GetMapping("/returns/search")
    public String searchActiveReturnLoans(@RequestParam("barcode") String barcode, Model model) {
        String trimmedBarcode = (barcode != null) ? barcode.trim() : "";
        List<BorrowDetail> searchResults = loanService.findActiveLoansByBarcode(trimmedBarcode);

        model.addAttribute("searchResults", searchResults);
        model.addAttribute("searchedBarcode", trimmedBarcode);
        model.addAttribute("todayReturned", loanService.getTodayReturnedBooks());
        model.addAttribute("defaultReturnDate", LocalDate.now());

        if (searchResults.isEmpty()) {
            model.addAttribute("errorMessage", message("backend.return.activeNotFound", trimmedBarcode));
        } else {
            model.addAttribute("successMessage", message("backend.return.activeFound"));
        }
        return "librarian/return-desk";
    }

    /**
     * POST: /librarian/loan/returns/confirm
     * Tiếp nhận dữ liệu từ Modal gửi lên: cập nhật ngày trả thực tế, tình trạng vật lý, ghi chú, tính phạt quá hạn.
     */
    @PostMapping("/returns/confirm")
    public String confirmReturnBookWithDetails(@RequestParam("barcode") String barcode,
                                               @RequestParam("returnDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate returnDate,
                                               @RequestParam("conditionNote") String conditionNote,
                                               @RequestParam(value = "conditionNoteAdditional", required = false) String conditionNoteAdditional,
                                               Principal principal,
                                               RedirectAttributes redirectAttributes) {
        try {
            if (barcode == null || barcode.trim().isEmpty()) {
                throw new ValidationException(message("backend.return.invalidBarcode"));
            }

            String staffUsername = (principal != null) ? principal.getName() : "admin";

            // Hợp nhất nội dung lựa chọn và ghi chú bổ sung của Thủ thư làm chuỗi lưu vết tình trạng sách
            String fullConditionLog = conditionNote;
            if (conditionNoteAdditional != null && !conditionNoteAdditional.trim().isEmpty()) {
                fullConditionLog += message("backend.return.conditionDetail", conditionNoteAdditional.trim());
            }

            // Chuyển LocalDate sang LocalDateTime (giữ nguyên giờ, phút, giây hiện hành của ngày làm việc)
            LocalDateTime actualReturnDateTime = returnDate.atTime(LocalDateTime.now().toLocalTime());

            // Thực thi nghiệp vụ lõi trong LoanService
            loanService.confirmReturnWithDetails(barcode.trim(), actualReturnDateTime, fullConditionLog, staffUsername);

            redirectAttributes.addFlashAttribute("successMessage", message("backend.return.confirmed"));
        } catch (ApplicationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", messageWithDetail("backend.return.failed", e));
        }
        return "redirect:/librarian/loan/returns";
    }

    /**
     * GET: /librarian/loan/borrow-schedule
     * Tích hợp điều hướng tab xem danh sách lịch mượn trả chi tiết.
     */
    @GetMapping("/borrow-schedule")
    public String showBorrowSchedule(Model model) {
        model.addAttribute("details", loanService.getAllBorrowDetails());
        return "librarian/borrow-schedule";
    }

    /**
     * GET: /librarian/loan/borrow-schedule/detail-preview
     * Giao diện UI cứng xem trước chi tiết (Preview).
     */
    @GetMapping("/borrow-schedule/detail-preview")
    public String showBorrowScheduleDetailPreview() {
        return "librarian/borrow-schedule-detail";
    }

    /**
     * POST: /librarian/loan/renew/{id}
     * Gia hạn thủ công bởi thủ thư.
     */
    @PostMapping("/renew/{id}")
    public String manualRenew(@PathVariable("id") Integer borrowDetailId, RedirectAttributes redirectAttributes) {
        try {
            loanService.processRenewal(borrowDetailId);
            redirectAttributes.addFlashAttribute("successMessage", message("backend.renewal.success"));
        } catch (ApplicationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", messageWithDetail("backend.renewal.failed", e));
        }
        return "redirect:/librarian/loan/borrow-schedule";
    }

    /**
     * POST: /librarian/loan/renew/approve/{id}
     * Duyệt yêu cầu gia hạn từ độc giả.
     */
    @PostMapping("/renew/approve/{id}")
    public String approveRenewal(@PathVariable("id") Integer borrowDetailId, Principal principal, RedirectAttributes redirectAttributes) {
        try {
            String staffUsername = (principal != null) ? principal.getName() : "admin";
            loanService.approveRenewal(borrowDetailId, staffUsername);
            redirectAttributes.addFlashAttribute("successMessage", message("backend.renewal.approved"));
        } catch (ApplicationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", messageWithDetail("backend.renewal.approveFailed", e));
        }
        return "redirect:/librarian/borrow/create";
    }

    /**
     * POST: /librarian/loan/renew/reject/{id}
     * Từ chối yêu cầu gia hạn từ độc giả.
     */
    @PostMapping("/renew/reject/{id}")
    public String rejectRenewal(@PathVariable("id") Integer borrowDetailId, Principal principal, RedirectAttributes redirectAttributes) {
        try {
            String staffUsername = (principal != null) ? principal.getName() : "admin";
            loanService.rejectRenewal(borrowDetailId, staffUsername);
            redirectAttributes.addFlashAttribute("successMessage", message("backend.renewal.rejected"));
        } catch (ApplicationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", messageWithDetail("backend.renewal.rejectFailed", e));
        }
        return "redirect:/librarian/borrow/create";
    }
}
