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

    // Hiển thị Form tạo phiếu mượn sách mới
    @GetMapping("/create")
    public String showCreateBorrowForm(Model model) {
        model.addAttribute("borrowRequest", new BorrowRequest());
        return "librarian/create-borrow";
    }

    // Xử lý gửi Form tạo phiếu mượn sách
    @PostMapping("/create")
    public String processCreateBorrow(@ModelAttribute("borrowRequest") BorrowRequest request,
                                      @RequestParam("rawBarcodes") String rawBarcodes,
                                      Principal principal,
                                      RedirectAttributes redirectAttributes) {
        try {
            // Cắt chuỗi mã vạch cách nhau bằng dấu phẩy từ giao diện nhập vào list string
            if (rawBarcodes != null && !rawBarcodes.trim().isEmpty()) {
                request.setBarcodes(Arrays.asList(rawBarcodes.split("\\s*,\\s*")));
            }

            String librarianUsername = (principal != null) ? principal.getName() : "admin";

            // Gọi tầng nghiệp vụ xử lý logic
            borrowService.processBorrowing(request, librarianUsername);

            redirectAttributes.addFlashAttribute("successMessage", "Tạo phiếu mượn sách thành công thành công!");
            return "redirect:/librarian/borrow/create?success";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Thất bại: " + e.getMessage());
            return "redirect:/librarian/borrow/create";
        }
    }
}