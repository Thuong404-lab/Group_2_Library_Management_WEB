package com.lms.controller.member;

import com.lms.dto.response.MemberBorrowDTO;
import com.lms.entity.Book;
import com.lms.service.BorrowService;
import com.lms.service.BookService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/member/borrow")
public class BorrowController {

    private final BorrowService borrowService;
    private final BookService bookService;
    private final com.lms.service.LoanService loanService;

    public BorrowController(BorrowService borrowService,
                            BookService bookService,
                            com.lms.service.LoanService loanService) {
        this.borrowService = borrowService;
        this.bookService = bookService;
        this.loanService = loanService;
    }

    @GetMapping("/management")
    public String viewBorrowManagement(@RequestParam(value = "tab", defaultValue = "borrowing") String tab,
                                       Principal principal, Model model) {
        if (principal == null) return "redirect:/login";

        String username = principal.getName();
        model.addAttribute("activeTab", tab);

        // Lấy dữ liệu để đếm số lượng hiển thị Badge động trên Tab Pills
        List<MemberBorrowDTO> currentBorrows = borrowService.getMemberCurrentBorrows(username);
        List<MemberBorrowDTO> reservations = borrowService.getMemberReservations(username);

        model.addAttribute("borrowingCount", currentBorrows.size());
        model.addAttribute("reservationCount", reservations.size());

        // Lựa chọn nạp dữ liệu phù hợp với Tab hiện tại
        if ("reserved".equalsIgnoreCase(tab)) {
            model.addAttribute("booksData", reservations);
        } else if ("history".equalsIgnoreCase(tab)) {
            model.addAttribute("booksData", borrowService.getMemberOneMonthHistory(username));
        } else {
            model.addAttribute("booksData", currentBorrows);
        }

        return "member/borrow"; // Trả về tệp tin template member/borrow.html của bạn
    }

    @GetMapping("/create")
    public String showCreateRequestForm(@RequestParam(value = "bookId", required = false) Integer bookId, Model model, Principal principal,
                                        RedirectAttributes redirectAttributes) {
        if (principal == null) return "redirect:/login";

        if (bookId == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng chọn sách trước khi gửi yêu cầu mượn.");
            return "redirect:/";
        }

        model.addAttribute("currentMemberName", principal.getName());
        model.addAttribute("selectedBookId", bookId);

        try {
            Book book = bookService.findBookById(bookId);
            model.addAttribute("selectedBook", book);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Sách không hợp lệ. Vui lòng thử lại.");
            return "redirect:/";
        }
        return "member/borrow-create";
    }

    @PostMapping("/request/submit")
    public String submitBorrowRequest(@RequestParam(value = "bookId", required = false) Integer bookId,
                                      @RequestParam(value = "numberOfDays", defaultValue = "14") Integer numberOfDays,
                                      Principal principal,
                                      RedirectAttributes redirectAttributes) {
        if (principal == null) return "redirect:/login";
        if (bookId == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn chưa chọn sách để gửi yêu cầu mượn.");
            return "redirect:/";
        }
        try {
            borrowService.memberSubmitBorrowRequest(principal.getName(), bookId, numberOfDays);
            redirectAttributes.addFlashAttribute("successMessage", "Đăng ký thành công! Yêu cầu của bạn đang được chờ phê duyệt.");
            return "redirect:/member/borrow/management?tab=borrowing";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể tạo yêu cầu mượn: " + e.getMessage());
            return "redirect:/member/borrow/create?bookId=" + bookId;
        }
    }

    @GetMapping("/reserve/form/{bookId}")
    public String showReserveForm(@PathVariable Integer bookId, Model model, Principal principal) {
        if (principal == null) return "redirect:/login";
        try {
            Book book = bookService.findBookById(bookId);
            model.addAttribute("book", book);
            model.addAttribute("username", principal.getName());
            return "member/reserve-confirm";
        } catch (Exception e) {
            return "redirect:/";
        }
    }

    // FIX VẤN ĐỀ 4: Chuyển hướng xử lý qua borrowService.memberSubmitReservationRequest để validate chặt chẽ
    @PostMapping("/reserve/{bookId}")
    public String reserveBook(@PathVariable Integer bookId, Principal principal, RedirectAttributes redirectAttributes) {
        if (principal == null) return "redirect:/login";
        try {
            borrowService.memberSubmitReservationRequest(principal.getName(), bookId);
            redirectAttributes.addFlashAttribute("successMessage", "Đặt giữ chỗ sách thành công! Vui lòng chờ Thủ thư phê duyệt.");
            return "redirect:/member/borrow/management?tab=reserved";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi đặt chỗ: " + e.getMessage());
            return "redirect:/member/borrow/management?tab=reserved";
        }
    }

    // FIX VẤN ĐỀ 5: Thêm endpoint xử lý Hủy đặt trước sách từ Member
    @PostMapping("/cancel-reservation/{reservationId}")
    public String cancelReservation(@PathVariable Integer reservationId, Principal principal, RedirectAttributes redirectAttributes) {
        if (principal == null) return "redirect:/login";
        try {
            borrowService.memberCancelReservation(principal.getName(), reservationId);
            redirectAttributes.addFlashAttribute("successMessage", "Hủy yêu cầu đặt giữ chỗ thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể hủy đơn đặt chỗ: " + e.getMessage());
        }
        return "redirect:/member/borrow/management?tab=reserved";
    }

    @PostMapping("/return/{loanId}")
    public String returnBook(@PathVariable("loanId") Integer loanId, Principal principal, RedirectAttributes redirectAttributes) {
        if (principal == null) return "redirect:/login";
        try {
            borrowService.memberSubmitReturnRequest(principal.getName(), loanId);
            redirectAttributes.addFlashAttribute("successMessage", "Yêu cầu trả sách đã được gửi tới Thủ thư thành công.");
            return "redirect:/member/borrow/management?tab=borrowing";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi xử lý gửi yêu cầu: " + e.getMessage());
            return "redirect:/member/borrow/management?tab=borrowing";
        }
    }

    @PostMapping("/renew/{borrowDetailId}")
    public String renewBook(@PathVariable("borrowDetailId") Integer borrowDetailId, Principal principal, RedirectAttributes redirectAttributes) {
        if (principal == null) return "redirect:/login";
        try {
            loanService.processRenewal(borrowDetailId);
            redirectAttributes.addFlashAttribute("successMessage", "Gia hạn thành công thêm 7 ngày!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi gia hạn: " + e.getMessage());
        }
        return "redirect:/member/borrow";
    }

    @GetMapping("/history")
    public String viewBorrowingHistory() {
        return "redirect:/member/borrow/management?tab=history";
    }

    @GetMapping("/current")
    public String viewCurrentBorrows() {
        return "redirect:/member/borrow/management?tab=borrowing";
    }

    @GetMapping("/returns")
    public String viewPendingReturns() {
        return "redirect:/member/borrow/management?tab=borrowing";
    }
}