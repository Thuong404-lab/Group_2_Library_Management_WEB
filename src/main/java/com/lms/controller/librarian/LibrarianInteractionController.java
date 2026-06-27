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
import com.lms.dto.request.LibrarianNotificationSendRequest;
import org.springframework.validation.BindingResult;

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

        try {
            // ======= ĐÃ SỬA =======
            // Bỏ lấy Authentication và username
            librarianInteractionService.moderateReview(feedbackId, request);

            String actionMsg = "APPROVE".equalsIgnoreCase(request.getAction()) ? "duyệt" : "từ chối";
            flash.addFlashAttribute("success", "Đã " + actionMsg + " đánh giá thành công.");
        } catch (Exception e) {
            flash.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }

        return "redirect:/librarian/interaction/reviews";
    }

    @GetMapping("/notifications/new")
    public String notificationForm(Model model) {

        model.addAttribute(
                "notificationRequest",
                new LibrarianNotificationSendRequest());

        model.addAttribute("members", librarianInteractionService.getAllMembers());

        return "librarian/send-notification";
    }

    @PostMapping("/notifications")
    public String sendNotificationToMembers(
            @ModelAttribute("notificationRequest") LibrarianNotificationSendRequest request,
            Model model,
            RedirectAttributes flash) {

        try {
            librarianInteractionService.sendNotificationToMembers(request);
            flash.addFlashAttribute("success", "Đã gửi thông báo thành công.");
            return "redirect:/librarian/interaction/notifications/new";

        } catch (Exception e) {
            model.addAttribute("error", "Lỗi: " + e.getMessage());
            model.addAttribute("members", librarianInteractionService.getAllMembers());

            // Giữ lại dữ liệu đã nhập vì request vẫn nằm trong model
            return "librarian/send-notification";
        }
    }
}