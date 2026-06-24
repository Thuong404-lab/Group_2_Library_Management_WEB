package com.lms.controller.librarian;

import com.lms.dto.request.LibrarianReviewModerateRequest;
import com.lms.service.LibrarianInteractionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/librarian/interaction")
public class LibrarianInteractionController {

    private final LibrarianInteractionService librarianInteractionService;

    // Constructor Injection (Không Lombok)
    public LibrarianInteractionController(LibrarianInteractionService librarianInteractionService) {
        this.librarianInteractionService = librarianInteractionService;
    }

    @GetMapping("/reviews")
    public String viewReviewsForModeration(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        model.addAttribute("reviews", librarianInteractionService.getReviewsForModeration(
                status, PageRequest.of(page, 20, Sort.by("createdDate").ascending())));

        model.addAttribute("currentStatus", (status == null || status.isBlank()) ? "PENDING" : status);
        return "librarian/reviews-moderation";
    }

    @PostMapping("/reviews/{id}/moderate")
    public String moderateReview(
            @PathVariable("id") Integer feedbackId,
            @Valid @ModelAttribute LibrarianReviewModerateRequest request,
            RedirectAttributes flash) {

        Integer currentStaffId = 2; // Giả lập Staff ID đang đăng nhập

        try {
            librarianInteractionService.moderateReview(feedbackId, request, currentStaffId);
            String actionMsg = "APPROVE".equalsIgnoreCase(request.getAction()) ? "duyệt" : "từ chối";
            flash.addFlashAttribute("success", "Đã " + actionMsg + " đánh giá thành công. Thông báo đã được gửi đến độc giả.");
        } catch (Exception e) {
            flash.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }

        return "redirect:/librarian/interaction/reviews";
    }
}