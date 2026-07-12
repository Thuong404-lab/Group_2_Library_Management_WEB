package com.lms.controller.librarian;

import com.lms.dto.request.LibrarianNotificationSendRequest;
import com.lms.dto.request.LibrarianReviewReplyRequest;
import com.lms.enums.NotificationRecipientType;
import com.lms.service.LibrarianInteractionService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/librarian/interaction")
public class LibrarianInteractionController {

    private static final int PAGE_SIZE = 10;

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
                status, PageRequest.of(Math.max(0, page), PAGE_SIZE, Sort.by("createdDate").descending())));

        return "librarian/reviews-response";
    }

    @PostMapping("/reviews/{id}/reply")
    public String replyReview(
            @PathVariable("id") Integer feedbackId,
            @ModelAttribute LibrarianReviewReplyRequest request,
            RedirectAttributes flash) {

        String response = request.getResponse() == null ? "" : request.getResponse().trim();

        if (response.isEmpty()) {
            flash.addFlashAttribute("reviewReplyErrorId", feedbackId);
            flash.addFlashAttribute("reviewReplyErrors",
                    Map.of("response", "Nội dung phản hồi không được để trống."));
            flash.addFlashAttribute("reviewReplyValues", Map.of("response", response));
            return "redirect:/librarian/dashboard?section=reviews";
        }

        request.setResponse(response);

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

        Map<String, String> fieldErrors = validateNotificationRequest(request);

        if (!fieldErrors.isEmpty()) {
            flash.addFlashAttribute("notificationRequest", request);
            flash.addFlashAttribute("notificationFieldErrors", fieldErrors);
            return "redirect:/librarian/dashboard?section=notifications";
        }

        try {
            librarianInteractionService.sendNotificationToMembers(request);
            flash.addFlashAttribute("success", "Đã gửi thông báo thành công.");
        } catch (Exception e) {
            flash.addFlashAttribute("notificationRequest", request);
            flash.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }

        return "redirect:/librarian/dashboard?section=notifications";
    }

    @GetMapping("/acquisition-requests")
    public String viewBookAcquisitionRequests(
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        model.addAttribute("requests", librarianInteractionService.getBookAcquisitionRequests(
                PageRequest.of(Math.max(0, page), PAGE_SIZE, Sort.by("requestId").ascending())));

        return "librarian/acquisition-request-list";
    }

    private Map<String, String> validateNotificationRequest(LibrarianNotificationSendRequest request) {
        Map<String, String> fieldErrors = new HashMap<>();

        if (request.getRecipientType() == null) {
            fieldErrors.put("recipientType", "Vui lòng chọn đối tượng nhận thông báo.");
        }

        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            fieldErrors.put("title", "Tiêu đề không được để trống.");
        } else {
            request.setTitle(request.getTitle().trim());
        }

        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            fieldErrors.put("content", "Nội dung không được để trống.");
        } else {
            request.setContent(request.getContent().trim());
        }

        if (request.getRecipientType() == NotificationRecipientType.SELECTED
                && (request.getMemberIds() == null || request.getMemberIds().isEmpty())) {
            fieldErrors.put("memberIds", "Vui lòng chọn ít nhất một Member.");
        }

        return fieldErrors;
    }
}
