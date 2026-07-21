package com.lms.controller.librarian;

import com.lms.config.CustomUserDetails;
import com.lms.controller.LocalizedControllerSupport;
import com.lms.exception.ApplicationException;

import com.lms.dto.request.LibrarianNotificationSendRequest;
import com.lms.dto.request.LibrarianReviewReplyRequest;
import com.lms.enums.NotificationType;
import com.lms.service.LibrarianInteractionService;
import com.lms.service.NotificationComposePolicy;
import com.lms.dto.response.NotificationRecipientSearchResponse;
import com.lms.dto.response.NotificationSendResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
            @RequestParam(defaultValue = "0") int page,
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
            return reviewRedirect(page);
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

        return reviewRedirect(page);
    }

    @PostMapping("/reviews/{id}/delete")
    public String deleteReview(
            @PathVariable("id") Integer feedbackId,
            @RequestParam(defaultValue = "0") int page,
            RedirectAttributes flash) {

        try {
            librarianInteractionService.deleteReview(feedbackId);
            flash.addFlashAttribute("success", message("backend.librarian.review.deleted"));
        } catch (ApplicationException e) {
            flash.addFlashAttribute("error", message("backend.errorWithDetail", e.getMessage()));
        }

        return reviewRedirect(page);
    }

    @GetMapping("/notifications/new")
    public String notificationForm(
            Model model,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (!model.containsAttribute("notificationRequest")) {
            LibrarianNotificationSendRequest request = new LibrarianNotificationSendRequest();
            request.setRequestToken(UUID.randomUUID().toString());
            model.addAttribute("notificationRequest", request);
        }
        model.addAttribute("notificationTypes", NotificationType.manualSelectableValues());
        LibrarianNotificationSendRequest request =
                (LibrarianNotificationSendRequest) model.getAttribute("notificationRequest");
        if (request.getRequestToken() == null) request.setRequestToken(UUID.randomUUID().toString());
        model.addAttribute("selectedMembers", librarianInteractionService.getNotificationRecipients(request.getMemberIds()));
        model.addAttribute("activeMemberCount", librarianInteractionService.countActiveMembers());
        model.addAttribute("recentNotifications", librarianInteractionService.getRecentManualNotifications());
        model.addAttribute("notificationTitleMin", NotificationComposePolicy.TITLE_MIN_LENGTH);
        model.addAttribute("notificationTitleMax", NotificationComposePolicy.TITLE_MAX_LENGTH);
        model.addAttribute("notificationContentMin", NotificationComposePolicy.CONTENT_MIN_LENGTH);
        model.addAttribute("notificationContentMax", NotificationComposePolicy.CONTENT_MAX_LENGTH);
        model.addAttribute("notificationSearchMin", NotificationComposePolicy.MEMBER_SEARCH_MIN_LENGTH);
        model.addAttribute("notificationMaxRecipients", NotificationComposePolicy.MAX_SELECTED_RECIPIENTS);
        if (userDetails != null && userDetails.getUser() != null) {
            model.addAttribute("currentUser", userDetails.getUser());
        }

        return "librarian/send-notification";
    }

    @GetMapping("/notifications/recipients")
    @ResponseBody
    public Page<NotificationRecipientSearchResponse> searchNotificationRecipients(
            @RequestParam(defaultValue = "") String query,
            @RequestParam(defaultValue = "0") int page) {
        return librarianInteractionService.searchNotificationRecipients(query,
                PageRequest.of(Math.max(0, page), NotificationComposePolicy.MEMBER_SEARCH_PAGE_SIZE));
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
            if (principal == null) {
                throw new com.lms.exception.ValidationException(
                        message("backend.librarian.notification.senderNotFound"));
            }
            NotificationSendResult result =
                    librarianInteractionService.sendNotificationToMembers(request, principal.getName());
            flash.addFlashAttribute("success", message(
                    result.duplicateRequest()
                            ? "backend.librarian.notification.duplicate"
                            : "backend.librarian.notification.sent",
                    result.notificationId(), result.recipientCount()));
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
                PageRequest.of(Math.max(0, page), PAGE_SIZE,
                        Sort.by(Sort.Order.desc("createdDate"), Sort.Order.desc("requestId")))));

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
        NotificationComposePolicy.normalizeAndValidate(request)
                .forEach((field, key) -> fieldErrors.put(field, message(key,
                        NotificationComposePolicy.MAX_SELECTED_RECIPIENTS)));
        return fieldErrors;
    }

    private String reviewRedirect(int page) {
        return "redirect:/librarian/interaction/reviews?page=" + Math.max(0, page);
    }

}
