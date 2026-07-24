package com.lms.controller.librarian;

import com.lms.config.CustomUserDetails;
import com.lms.controller.LocalizedControllerSupport;
import com.lms.exception.ApplicationException;

import com.lms.dto.request.LibrarianNotificationSendRequest;
import com.lms.dto.request.LibrarianReviewReplyRequest;
import com.lms.enums.NotificationType;
import com.lms.enums.FeedbackStatus;
import com.lms.enums.AcquisitionRequestStatus;
import com.lms.util.ReviewPolicy;
import com.lms.util.AcquisitionRequestPolicy;
import com.lms.service.LibrarianInteractionService;
import jakarta.validation.Valid;
import com.lms.service.NotificationComposePolicy;
import com.lms.dto.response.NotificationRecipientSearchResponse;
import com.lms.dto.response.NotificationSendResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
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
import java.util.Locale;
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
                status, PageRequest.of(Math.max(0, page), ReviewPolicy.PAGE_SIZE,
                        Sort.by(Sort.Order.desc("createdDate"), Sort.Order.desc("feedbackId")))));
        model.addAttribute("reviewStatuses", FeedbackStatus.values());
        model.addAttribute("currentStatus", status == null ? "" : status.strip().toUpperCase(Locale.ROOT));

        return "librarian/reviews-response";
    }

    @PostMapping("/reviews/{id}/reply")
    public String replyReview(
            @PathVariable("id") Integer feedbackId,
            @Valid @ModelAttribute LibrarianReviewReplyRequest request,
            BindingResult bindingResult,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String status,
            RedirectAttributes flash) {

        String response = request.getResponse() == null ? "" : request.getResponse().strip()
                .replaceAll("(?:\\R\\s*){3,}", System.lineSeparator() + System.lineSeparator());

        String responseError = null;
        if (response.isEmpty()) {
            responseError = message("backend.librarian.reviewReply.required");
        } else if (response.length() < ReviewPolicy.CONTENT_MIN_LENGTH) {
            responseError = message("backend.librarian.reviewReply.minimum");
        } else if (response.length() > ReviewPolicy.CONTENT_MAX_LENGTH) {
            responseError = message("backend.librarian.reviewReply.maximum");
        } else if (response.codePoints().noneMatch(Character::isLetter)) {
            responseError = message("backend.librarian.reviewReply.letters");
        } else if (bindingResult.hasErrors()) {
            responseError = bindingResult.getAllErrors().get(0).getDefaultMessage();
        }

        if (responseError != null) {
            flash.addFlashAttribute("reviewReplyErrorId", feedbackId);
            flash.addFlashAttribute("reviewReplyErrors", Map.of("response", responseError));
            flash.addFlashAttribute("reviewReplyValues", Map.of("response", response));
            return reviewRedirect(page, status);
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

        return reviewRedirect(page, status);
    }

    @PostMapping("/reviews/{id}/approve")
    public String approveReview(
            @PathVariable("id") Integer feedbackId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String status,
            RedirectAttributes flash) {

        try {
            librarianInteractionService.approveReview(feedbackId);
            flash.addFlashAttribute("success", message("backend.librarian.review.approved"));
        } catch (ApplicationException e) {
            flash.addFlashAttribute("error", message("backend.errorWithDetail", e.getMessage()));
        }

        return reviewRedirect(page, status);
    }

    @PostMapping("/reviews/{id}/reject")
    public String rejectReview(
            @PathVariable("id") Integer feedbackId,
            @RequestParam(required = false) String reason,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String status,
            RedirectAttributes flash) {
        try {
            librarianInteractionService.rejectReview(feedbackId, reason);
            flash.addFlashAttribute("success", message("backend.librarian.review.rejected"));
        } catch (ApplicationException e) {
            flash.addFlashAttribute("error", message("backend.errorWithDetail", e.getMessage()));
        }
        return reviewRedirect(page, status);
    }

    @PostMapping("/reviews/{id}/delete")
    public String deleteReview(
            @PathVariable("id") Integer feedbackId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String status,
            RedirectAttributes flash) {
        try {
            librarianInteractionService.deleteReview(feedbackId);
            flash.addFlashAttribute("success", message("backend.librarian.review.deleted"));
        } catch (ApplicationException e) {
            flash.addFlashAttribute("error", message("backend.errorWithDetail", e.getMessage()));
        }
        return reviewRedirect(page, status);
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
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        model.addAttribute("requests", librarianInteractionService.getBookAcquisitionRequests(
                status, keyword, PageRequest.of(Math.max(0, page), AcquisitionRequestPolicy.PAGE_SIZE,
                        Sort.by(Sort.Order.desc("createdDate"), Sort.Order.desc("requestId")))));
        model.addAttribute("acquisitionStatuses", AcquisitionRequestStatus.values());
        model.addAttribute("currentAcquisitionStatus",
                status == null ? "" : status.strip().toUpperCase(Locale.ROOT));
        model.addAttribute("acquisitionKeyword", keyword == null ? "" : keyword.strip());

        return "librarian/acquisition-request-list";
    }

    @PostMapping("/acquisition-requests/{id}/approve")
    public String approveBookAcquisitionRequest(@PathVariable("id") Integer requestId,
                                                @RequestParam("note") String note,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(required = false) String status,
                                                @RequestParam(required = false) String keyword,
                                                Principal principal,
                                                RedirectAttributes flash) {
        try {
            librarianInteractionService.approveBookAcquisitionRequest(
                    requestId, note, principal.getName());
            flash.addFlashAttribute("success", message("backend.librarian.acquisition.approved"));
        } catch (ApplicationException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return acquisitionRedirect(page, status, keyword, flash);
    }

    @PostMapping("/acquisition-requests/{id}/reject")
    public String rejectBookAcquisitionRequest(@PathVariable("id") Integer requestId,
                                               @RequestParam("reason") String reason,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(required = false) String status,
                                               @RequestParam(required = false) String keyword,
                                               Principal principal,
                                               RedirectAttributes flash) {
        try {
            librarianInteractionService.rejectBookAcquisitionRequest(
                    requestId, reason, principal.getName());
            flash.addFlashAttribute("success", message("backend.librarian.acquisition.rejected"));
        } catch (ApplicationException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return acquisitionRedirect(page, status, keyword, flash);
    }

    private Map<String, String> validateNotificationRequest(LibrarianNotificationSendRequest request) {
        Map<String, String> fieldErrors = new HashMap<>();
        NotificationComposePolicy.normalizeAndValidate(request)
                .forEach((field, key) -> fieldErrors.put(field, message(key,
                        NotificationComposePolicy.MAX_SELECTED_RECIPIENTS)));
        return fieldErrors;
    }

    private String reviewRedirect(int page, String status) {
        String redirect = "redirect:/librarian/interaction/reviews?page=" + Math.max(0, page);
        if (status == null || status.isBlank()) {
            return redirect;
        }
        try {
            return redirect + "&status=" + FeedbackStatus
                    .valueOf(status.strip().toUpperCase(Locale.ROOT)).name();
        } catch (IllegalArgumentException exception) {
            return redirect;
        }
    }

    private String acquisitionRedirect(int page, String status, String keyword,
                                       RedirectAttributes attributes) {
        attributes.addAttribute("page", Math.max(0, page));
        if (status != null && !status.isBlank()) {
            try {
                attributes.addAttribute("status", AcquisitionRequestStatus
                        .valueOf(status.strip().toUpperCase(Locale.ROOT)).name());
            } catch (IllegalArgumentException ignored) {
                // Do not propagate a tampered filter value into the redirect URL.
            }
        }
        if (keyword != null && !keyword.isBlank()) {
            attributes.addAttribute("keyword", keyword.strip());
        }
        return "redirect:/librarian/interaction/acquisition-requests";
    }

}
