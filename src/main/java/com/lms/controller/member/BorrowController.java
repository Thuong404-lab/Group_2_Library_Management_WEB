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

    // UC-6.0: Hiển thị form tạo yêu cầu mượn sách
    @GetMapping("/create")
    public String showCreateRequestForm(Model model, Principal principal) {
        // Lấy thông tin user đăng nhập
        model.addAttribute("currentMemberName", (principal != null) ? principal.getName() : "Khách");
        // Ở đây bạn sẽ thêm model.addAttribute("availableBooks", bookService.findAll());
        return "member/borrow-create"; // Đảm bảo bạn có file templates/member/borrow-create.html
    }

    // UC-6.0: Xử lý submit yêu cầu mượn
    @PostMapping("/request/submit")
    public String submitBorrowRequest(@RequestParam String requestDate,
                                      @RequestParam String returnDueDate,
                                      @RequestParam(required = false) String notes,
                                      Principal principal) {
        // TODO: Gọi Service lưu vào DB với status = PENDING
        return "redirect:/member/borrow/history?success=true";
    }

    // UC-6.1: View Borrowing History
    @GetMapping("/history")
    public String viewBorrowingHistory(Principal principal, Model model) {
        return "member/borrow-history";
    }

    // UC-6.2: Reserve Books
    @PostMapping("/reserve/{bookId}")
    public String reserveBook(@PathVariable Integer bookId) {
        return "redirect:/books/" + bookId + "?reserved";
    }

    // UC-6.3 & 6.4: Các view còn lại
    @GetMapping("/current")
    public String viewCurrentBorrows() { return "member/current-borrows"; }

    @GetMapping("/returns")
    public String viewPendingReturns() { return "member/pending-returns"; }
}