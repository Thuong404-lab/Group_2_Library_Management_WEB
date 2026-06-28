package com.lms.controller.librarian;

import com.lms.dto.request.LibrarianNotificationSendRequest;
import com.lms.dto.request.LibrarianReviewReplyRequest;
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

    public LibrarianInteractionController(LibrarianInteractionService librarianInteractionService) {
        this.librarianInteractionService = librarianInteractionService;
    }

    @GetMapping("/reviews")
    public String viewReviewsForModeration(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        model.addAttribute("reviews", librarianInteractionService.getReviewsForModeration(
                status, PageRequest.of(page, 20, Sort.by("createdDate").descending())));

        return "librarian/reviews-response";
    }

    @PostMapping("/reviews/{id}/reply")
    public String replyReview(
            @PathVariable("id") Integer feedbackId,
            @Valid @ModelAttribute LibrarianReviewReplyRequest request,
            RedirectAttributes flash) {

        try {
            librarianInteractionService.replyReview(feedbackId, request);
            flash.addFlashAttribute("success", "Đã phản hồi đánh giá thành công.");
        } catch (Exception e) {
            flash.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }

        return "redirect:/librarian/dashboard?section=reviews";
    }

    @PostMapping("/reviews/{id}/delete")
    public String deleteReview(
            @PathVariable("id") Integer feedbackId,
            RedirectAttributes flash) {

        try {
            librarianInteractionService.deleteReview(feedbackId);
            flash.addFlashAttribute("success", "Đã xoá đánh giá thành công.");
        } catch (Exception e) {
            flash.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }

        return "redirect:/librarian/dashboard?section=reviews";
    }

    @GetMapping("/notifications/new")
    public String notificationForm(Model model) {
        model.addAttribute("notificationRequest", new LibrarianNotificationSendRequest());
        model.addAttribute("members", librarianInteractionService.getAllMembers());

        return "librarian/send-notification";
    }

    @PostMapping("/notifications")
    public String sendNotificationToMembers(
            @ModelAttribute("notificationRequest") LibrarianNotificationSendRequest request,
            RedirectAttributes flash) {

        try {
            librarianInteractionService.sendNotificationToMembers(request);
            flash.addFlashAttribute("success", "Đã gửi thông báo thành công.");
        } catch (Exception e) {
            flash.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }

        return "redirect:/librarian/dashboard?section=notifications";
    }

    @GetMapping("/acquisition-requests")
    public String viewBookAcquisitionRequests(
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        model.addAttribute("requests", librarianInteractionService.getBookAcquisitionRequests(
                PageRequest.of(page, 20, Sort.by("createdDate").descending())));

        return "librarian/acquisition-request-list";
    }
}