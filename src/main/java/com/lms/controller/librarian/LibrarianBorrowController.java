package com.lms.controller.librarian;

import com.lms.dto.request.BorrowRequest;
import com.lms.service.BorrowService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.security.Principal;
import java.util.Arrays;

@Controller
@RequestMapping("/librarian/borrow")
public class LibrarianBorrowController {

    private final BorrowService borrowService;

    public LibrarianBorrowController(BorrowService borrowService) {
        this.borrowService = borrowService;
    }

    @GetMapping("/list")
    public String listAllBorrows(Model model) {
        // TODO: Đưa danh sách các phiếu mượn (gồm cả Chờ duyệt & Đang mượn) ra view quản lý của thủ thư
        return "librarian/borrow-list";
    }

    // CHỨC NĂNG DUYỆT ONLINE: Thủ thư duyệt đơn PENDING thành BORROWING
    @PostMapping("/approve/{borrowId}")
    public String approveMemberRequest(@PathVariable("borrowId") Integer borrowId, RedirectAttributes redirectAttributes) {
        try {
            // Thực hiện đổi status -> Đang mượn (BORROWING) trong hệ thống DB của nhóm bạn
            // borrowService.approvePendingRequest(borrowId);

            redirectAttributes.addFlashAttribute("successMessage", "Đã phê duyệt đơn đăng ký mượn trực tuyến thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Phê duyệt thất bại: " + e.getMessage());
        }
        return "redirect:/librarian/borrow/list";
    }

    @GetMapping("/create")
    public String showCreateBorrowForm(Model model) {
        model.addAttribute("borrowRequest", new BorrowRequest());
        return "librarian/create-borrow";
    }

    @PostMapping("/create")
    public String processCreateBorrow(@ModelAttribute("borrowRequest") BorrowRequest request,
                                      @RequestParam("rawBarcodes") String rawBarcodes,
                                      Principal principal,
                                      RedirectAttributes redirectAttributes) {
        try {
            if (rawBarcodes != null && !rawBarcodes.trim().isEmpty()) {
                request.setBarcodes(Arrays.asList(rawBarcodes.split("\\s*,\\s*")));
            }
            String librarianUsername = (principal != null) ? principal.getName() : "admin";
            borrowService.processBorrowing(request, librarianUsername);
            redirectAttributes.addFlashAttribute("successMessage", "Tạo phiếu mượn trực tiếp tại quầy thành công!");
            return "redirect:/librarian/borrow/create?success";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Thất bại: " + e.getMessage());
            return "redirect:/librarian/borrow/create";
        }
    }
}