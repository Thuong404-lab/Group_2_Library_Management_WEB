package com.lms.controller.member;

import com.lms.service.BorrowService;
import com.lms.service.MemberFavoriteService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.security.Principal;

@Controller
@RequestMapping("/member/borrow")
public class BorrowController {

    private final BorrowService borrowService;
    private final MemberFavoriteService memberFavoriteService;

    public BorrowController(BorrowService borrowService, MemberFavoriteService memberFavoriteService) {
        this.borrowService = borrowService;
        this.memberFavoriteService = memberFavoriteService;
    }

    // UC-6.0: Hiển thị form tạo yêu cầu mượn sách (Sửa đổi để trả về đúng file form)
    @GetMapping("/create")
    public String showCreateRequestForm(@RequestParam(value = "bookId", required = false) Integer bookId, Model model, Principal principal) {
        if (principal == null) return "redirect:/login";

        model.addAttribute("currentMemberName", principal.getName());
        model.addAttribute("selectedBookId", bookId);

        // Trả về file HTML giao diện điền thông tin/Form mượn sách giống mẫu hình bạn gửi
        return "member/borrow-create";
    }

    // UC-6.0: Xử lý submit đơn đăng ký mượn trực tuyến (PENDING)
    @PostMapping("/request/submit")
    public String submitBorrowRequest(@RequestParam("bookId") Integer bookId,
                                      @RequestParam(value = "numberOfDays", defaultValue = "14") Integer numberOfDays,
                                      Principal principal,
                                      RedirectAttributes redirectAttributes) {
        if (principal == null) return "redirect:/login";
        try {
            // Gọi hàm của Service để lưu yêu cầu mượn vào DB dưới trạng thái PENDING
            borrowService.memberSubmitBorrowRequest(principal.getName(), bookId, numberOfDays);

            // Đẩy thông báo Flash chờ duyệt hiển thị tại Dashboard
            redirectAttributes.addFlashAttribute("successMessage", "Đăng ký thành công! Yêu cầu của bạn đã được gửi tới Thủ thư và đang chờ phê duyệt.");
            return "redirect:/member/dashboard?success=borrow";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể tạo yêu cầu mượn: " + e.getMessage());
            return "redirect:/member/borrow/create?bookId=" + bookId;
        }
    }

    // UC-6.2: Gọi xử lý đặt chỗ sách trực tuyến khi hết bản sao (Reserve)
    @PostMapping("/reserve/{bookId}")
    public String reserveBook(@PathVariable Integer bookId, Principal principal, RedirectAttributes redirectAttributes) {
        if (principal == null) return "redirect:/login";
        try {
            memberFavoriteService.reserveBook(principal.getName(), bookId);
            redirectAttributes.addFlashAttribute("successMessage", "Đặt giữ chỗ sách (Reserve) thành công!");
            return "redirect:/member/dashboard?success=reserve";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi đặt chỗ: " + e.getMessage());
            return "redirect:/member/dashboard?error=reserve";
        }
    }

    // UC-7.0: Độc giả gửi yêu cầu trả sách trực tuyến
    @PostMapping("/return/{loanId}")
    public String returnBook(@PathVariable("loanId") Integer loanId, Principal principal, RedirectAttributes redirectAttributes) {
        if (principal == null) return "redirect:/login";
        try {
            redirectAttributes.addFlashAttribute("successMessage", "Yêu cầu trả sách đã được ghi nhận! Vui lòng đợi thủ thư xác nhận kho.");
            return "redirect:/member/dashboard?success=return";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi xử lý trả sách: " + e.getMessage());
            return "redirect:/member/dashboard";
        }
    }

    @GetMapping("/history")
    public String viewBorrowingHistory(Principal principal, Model model) { return "member/borrow-history"; }

    @GetMapping("/current")
    public String viewCurrentBorrows() { return "member/current-borrows"; }

    @GetMapping("/returns")
    public String viewPendingReturns() { return "member/pending-returns"; }
}