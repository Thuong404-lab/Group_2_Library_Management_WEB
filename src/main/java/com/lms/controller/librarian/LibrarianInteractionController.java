package com.lms.controller.librarian;
import com.lms.controller.LocalizedControllerSupport;
import com.lms.exception.ApplicationException;

import com.lms.dto.request.LibrarianNotificationSendRequest;
import com.lms.dto.request.LibrarianReviewReplyRequest;
import com.lms.enums.NotificationRecipientType;
import com.lms.enums.NotificationType;
import com.lms.exception.ResourceNotFoundException;
import com.lms.exception.ValidationException;
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

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/librarian/interaction")
public class LibrarianInteractionController extends LocalizedControllerSupport {

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

        String response = request.getResponse() == null ? "" : request.getResponse().strip()
                .replaceAll("(?:\\R\\s*){3,}", System.lineSeparator() + System.lineSeparator());

        String responseError = null;
        if (response.isEmpty()) {
            responseError = message("backend.librarian.reviewReply.required");
        } else if (response.length() < 5) {
            responseError = message("backend.librarian.reviewReply.minimum");
        } else if (response.length() > 1000) {
            responseError = message("backend.librarian.reviewReply.maximum");
        } else if (response.codePoints().noneMatch(Character::isLetter)) {
            responseError = message("backend.librarian.reviewReply.letters");
        }

        if (responseError != null) {
            flash.addFlashAttribute("reviewReplyErrorId", feedbackId);
            flash.addFlashAttribute("reviewReplyErrors", Map.of("response", responseError));
            flash.addFlashAttribute("reviewReplyValues", Map.of("response", response));
            return "redirect:/librarian/interaction/reviews";
        }

        request.setResponse(response);

        try {
            boolean isEditing = librarianInteractionService.replyReview(feedbackId, request);
            flash.addFlashAttribute("success", isEditing
                    ? message("backend.librarian.reviewReply.updated")
                    : message("backend.librarian.reviewReply.created"));
        } catch (ApplicationException e) {
            flash.addFlashAttribute("error", message("backend.errorWithDetail", e.getMessage()));
        }

        return "redirect:/librarian/interaction/reviews";
    }

    @PostMapping("/reviews/{id}/delete")
    public String deleteReview(
            @PathVariable("id") Integer feedbackId,
            RedirectAttributes flash) {

        try {
            librarianInteractionService.deleteReview(feedbackId);
            flash.addFlashAttribute("success", message("backend.librarian.review.deleted"));
        } catch (ApplicationException e) {
            flash.addFlashAttribute("error", message("backend.errorWithDetail", e.getMessage()));
        }

        return "redirect:/librarian/interaction/reviews";
    }

    @GetMapping("/notifications/new")
    public String notificationForm(Model model) {
        model.addAttribute("notificationRequest", new LibrarianNotificationSendRequest());
        model.addAttribute("notificationTypes", NotificationType.values());
        model.addAttribute("members", librarianInteractionService.getAllMembers());

        return "librarian/send-notification";
    }

    @PostMapping("/notifications")
    public String sendNotificationToMembers(
            @ModelAttribute("notificationRequest") LibrarianNotificationSendRequest request,
            Principal principal,
            RedirectAttributes flash) {

        Map<String, String> fieldErrors = validateNotificationRequest(request);

        if (!fieldErrors.isEmpty()) {
            flash.addFlashAttribute("notificationRequest", request);
            flash.addFlashAttribute("notificationFieldErrors", fieldErrors);
            return "redirect:/librarian/interaction/notifications/new";
        }

        try {
            librarianInteractionService.sendNotificationToMembers(request, principal.getName());
            flash.addFlashAttribute("success", message("backend.librarian.notification.sent"));
        } catch (ApplicationException e) {
            flash.addFlashAttribute("notificationRequest", request);
            flash.addFlashAttribute("error", message("backend.errorWithDetail", e.getMessage()));
        }

        return "redirect:/librarian/interaction/notifications/new";
    }

    @GetMapping("/acquisition-requests")
    public String viewBookAcquisitionRequests(
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        model.addAttribute("requests", librarianInteractionService.getBookAcquisitionRequests(
                PageRequest.of(Math.max(0, page), PAGE_SIZE, Sort.by("requestId").ascending())));

        return "librarian/acquisition-request-list";
    }

    @PostMapping("/acquisition-requests/{id}/approve")
    public String approveBookAcquisitionRequest(@PathVariable("id") Integer requestId,
                                                RedirectAttributes flash) {
        try {
            librarianInteractionService.approveBookAcquisitionRequest(requestId);
            flash.addFlashAttribute("success", message("backend.librarian.acquisition.approved"));
        } catch (ApplicationException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/librarian/interaction/acquisition-requests";
    }

    @PostMapping("/acquisition-requests/{id}/reject")
    public String rejectBookAcquisitionRequest(@PathVariable("id") Integer requestId,
                                               @RequestParam("reason") String reason,
                                               RedirectAttributes flash) {
        try {
            librarianInteractionService.rejectBookAcquisitionRequest(requestId, reason);
            flash.addFlashAttribute("success", message("backend.librarian.acquisition.rejected"));
        } catch (ApplicationException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/librarian/interaction/acquisition-requests";
    }

    private Map<String, String> validateNotificationRequest(LibrarianNotificationSendRequest request) {
        Map<String, String> fieldErrors = new HashMap<>();

        if (request.getRecipientType() == null) {
            fieldErrors.put("recipientType", message("backend.librarian.notification.recipientRequired"));
        }

        if (request.getNotificationType() == null) {
            fieldErrors.put("notificationType", message("backend.librarian.notification.typeRequired"));
        }

        String normalizedTitle = normalizeNotificationTitle(request.getTitle());
        String normalizedContent = normalizeNotificationContent(request.getContent());
        request.setTitle(normalizedTitle);
        request.setContent(normalizedContent);

        if (normalizedTitle.isEmpty()) {
            fieldErrors.put("title", message("backend.librarian.notification.titleRequired"));
        } else if (normalizedTitle.length() < 5) {
            fieldErrors.put("title", message("backend.librarian.notification.titleMinimum"));
        } else if (normalizedTitle.length() > 150) {
            fieldErrors.put("title", message("backend.librarian.notification.titleMaximum"));
        }

        if (normalizedContent.isEmpty()) {
            fieldErrors.put("content", message("backend.librarian.notification.contentRequired"));
        } else if (normalizedContent.length() < 10) {
            fieldErrors.put("content", message("backend.librarian.notification.contentMinimum"));
        } else if (normalizedContent.length() > 2000) {
            fieldErrors.put("content", message("backend.librarian.notification.contentMaximum"));
        } else if (!normalizedTitle.isEmpty() && normalizedContent.equalsIgnoreCase(normalizedTitle)) {
            fieldErrors.put("content", message("backend.librarian.notification.contentDifferent"));
        }

        if (request.getRecipientType() == NotificationRecipientType.SELECTED
                && (request.getMemberIds() == null || request.getMemberIds().isEmpty())) {
            fieldErrors.put("memberIds", message("backend.librarian.notification.memberRequired"));
        }

        return fieldErrors;
    }

    private String normalizeNotificationTitle(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }

    private String normalizeNotificationContent(String value) {
        return value == null ? "" : value.strip().replaceAll("(?:\\R\\s*){3,}", System.lineSeparator() + System.lineSeparator());
    }

}
