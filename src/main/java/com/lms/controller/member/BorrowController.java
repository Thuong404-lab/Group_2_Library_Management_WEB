package com.lms.controller.member;

import com.lms.entity.Book;
import com.lms.service.BorrowService;
import com.lms.service.MemberFavoriteService;
import com.lms.service.BookService;
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
    private final BookService bookService;

    public BorrowController(BorrowService borrowService,
                            MemberFavoriteService memberFavoriteService,
                            BookService bookService) {
        this.borrowService = borrowService;
        this.memberFavoriteService = memberFavoriteService;
        this.bookService = bookService;
    }

    // UC-6.0: Hiển thị form tạo yêu cầu mượn sách trực tuyến
    @GetMapping("/create")
    public String showCreateRequestForm(@RequestParam(value = "bookId", required = false) Integer bookId, Model model, Principal principal,
                                        RedirectAttributes redirectAttributes) {
        if (principal == null) return "redirect:/login";

        if (bookId == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng chọn sách trước khi gửi yêu cầu mượn.");
            return "redirect:/member/dashboard";
        }

        model.addAttribute("currentMemberName", principal.getName());
        model.addAttribute("selectedBookId", bookId);

        try {
            Book book = bookService.findBookById(bookId);
            model.addAttribute("selectedBook", book);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Sách không hợp lệ. Vui lòng thử lại.");
            return "redirect:/member/dashboard";
        }
        return "member/borrow-create";
    }

    // UC-6.0: Xử lý submit đơn đăng ký mượn trực tuyến (Lưu trạng thái Pending vào bảng Borrows)
    @PostMapping("/request/submit")
    public String submitBorrowRequest(@RequestParam(value = "bookId", required = false) Integer bookId,
                                      @RequestParam(value = "numberOfDays", defaultValue = "14") Integer numberOfDays,
                                      Principal principal,
                                      RedirectAttributes redirectAttributes) {
        if (principal == null) return "redirect:/login";
        if (bookId == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn chưa chọn sách để gửi yêu cầu mượn.");
            return "redirect:/member/dashboard";
        }
        try {
            borrowService.memberSubmitBorrowRequest(principal.getName(), bookId, numberOfDays);
            redirectAttributes.addFlashAttribute("successMessage", "Đăng ký thành công! Yêu cầu của bạn đã được gửi tới Thủ thư và đang chờ phê duyệt.");
            return "redirect:/member/dashboard?success=borrow";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể tạo yêu cầu mượn: " + e.getMessage());
            return "redirect:/member/borrow/create?bookId=" + (bookId != null ? bookId : "");
        }
    }

    // UC-6.2: Gọi xử lý đặt giữ chỗ sách trực tuyến (Reserve) -> Form trung gian
    @GetMapping("/reserve/form/{bookId}")
    public String showReserveForm(@PathVariable Integer bookId, Model model, Principal principal) {
        if (principal == null) return "redirect:/login";
        try {
            Book book = bookService.findBookById(bookId);
            model.addAttribute("book", book);
            model.addAttribute("username", principal.getName());
            return "member/reserve-confirm";
        } catch (Exception e) {
            return "redirect:/member/dashboard";
        }
    }

    // FIX LỖI 1: Thực hiện gửi yêu cầu đặt chỗ lưu trực tiếp vào bảng Reservations (Không lỗi 500)
    @PostMapping("/reserve/{bookId}")
    public String reserveBook(@PathVariable Integer bookId, Principal principal, RedirectAttributes redirectAttributes) {
        if (principal == null) return "redirect:/login";
        try {
            // Chuyển sang service quản lý Favorites/Reservation để xử lý đặt trước
            memberFavoriteService.reserveBook(principal.getName(), bookId);
            redirectAttributes.addFlashAttribute("successMessage", "Đặt giữ chỗ sách (Reserve) thành công! Yêu cầu đã được gửi tới Thủ thư và đang chờ phê duyệt.");
            return "redirect:/member/dashboard?success=reserve";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi đặt chỗ: " + e.getMessage());
            return "redirect:/member/dashboard?error=reserve";
        }
    }

    // UC-7.0: Độc giả gửi yêu cầu trả sách trực tuyến ra quầy
    @PostMapping("/return/{loanId}")
    public String returnBook(@PathVariable("loanId") Integer loanId, Principal principal, RedirectAttributes redirectAttributes) {
        if (principal == null) return "redirect:/login";
        try {
            borrowService.updateStatus(loanId, "Return_Pending");
            redirectAttributes.addFlashAttribute("successMessage", "Yêu cầu trả sách đã được gửi đi! Vui lòng đợi thủ thư tiếp nhận vật lý tại quầy.");
            return "redirect:/member/dashboard?success=return";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi xử lý trả sách: " + e.getMessage());
            return "redirect:/member/dashboard";
        }
    }

    @GetMapping("/history")
    public String viewBorrowingHistory(Principal principal, Model model) {
        if (principal != null) {
            model.addAttribute("historyBorrows", borrowService.getAllBorrowHistoryByMember(principal.getName()));
        }
        return "member/borrow-history";
    }

    @GetMapping("/current")
    public String viewCurrentBorrows() { return "member/current-borrows"; }

    @GetMapping("/returns")
    public String viewPendingReturns() { return "member/pending-returns"; }
}