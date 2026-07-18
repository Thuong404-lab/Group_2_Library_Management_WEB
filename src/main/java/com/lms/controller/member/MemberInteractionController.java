package com.lms.controller.member;
import com.lms.exception.ApplicationException;
import com.lms.controller.LocalizedControllerSupport;

import com.lms.repository.BookRepository;
import com.lms.service.MemberFavoriteService;
import com.lms.service.MemberNotificationService;
import com.lms.service.MemberReviewService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.Map;
import com.lms.dto.request.MemberReviewSubmitRequest;
import com.lms.dto.request.MemberReviewUpdateRequest;
import com.lms.exception.ResourceNotFoundException;
import com.lms.exception.ValidationException;
import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.lms.dto.request.MemberBookAcquisitionRequest;
import com.lms.service.MemberBookAcquisitionService;
import com.lms.enums.NotificationSource;
import com.lms.enums.NotificationType;

@Controller
@RequestMapping("/member/interaction")
public class MemberInteractionController extends LocalizedControllerSupport {

    private static final int PAGE_SIZE = 10;
    private static final int NOTIFICATION_PAGE_SIZE = 20;

    private final MemberNotificationService memberNotificationService;
    private final MemberReviewService memberReviewService;
    private final BookRepository bookRepository;
    private final MemberBookAcquisitionService memberBookAcquisitionService;
    private final MemberFavoriteService memberFavoriteService;

    public MemberInteractionController(MemberNotificationService memberNotificationService,
                                       MemberReviewService memberReviewService,
                                       BookRepository bookRepository,
                                       MemberBookAcquisitionService memberBookAcquisitionService,
                                       MemberFavoriteService memberFavoriteService) {
        this.memberNotificationService = memberNotificationService;
        this.memberReviewService = memberReviewService;
        this.bookRepository = bookRepository;
        this.memberBookAcquisitionService = memberBookAcquisitionService;
        this.memberFavoriteService = memberFavoriteService;
    }

    @GetMapping("/notifications")
    public String viewNotifications(Model model,
                                    Principal principal,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "all") String source,
                                    @RequestParam(defaultValue = "ALL") String type) {
        NotificationSource sourceFilter = parseNotificationSource(source);
        NotificationType typeFilter = parseNotificationType(type);
        var notificationPage = memberNotificationService.getMyNotifications(
                principal.getName(), sourceFilter, typeFilter,
                PageRequest.of(Math.max(0, page), NOTIFICATION_PAGE_SIZE));
        var notifications = notificationPage.getContent();
        model.addAttribute("notificationPage", notificationPage);
        model.addAttribute("notifications", notifications);
        model.addAttribute("totalNotificationCount",
                memberNotificationService.countMyNotifications(principal.getName()));
        model.addAttribute("systemNotificationCount",
                memberNotificationService.countMyNotificationsBySource(
                        principal.getName(), NotificationSource.SYSTEM));
        model.addAttribute("librarianNotificationCount",
                memberNotificationService.countMyNotificationsBySource(
                        principal.getName(), NotificationSource.LIBRARIAN));
        model.addAttribute("selectedNotificationSource",
                sourceFilter == null ? "all" : sourceFilter.name().toLowerCase());
        model.addAttribute("selectedNotificationType",
                typeFilter == null ? "ALL" : typeFilter.name());
        model.addAttribute("showNotificationBell", false);

        return "member/notifications";
    }

    @PostMapping("/notifications/mark-read")
    @ResponseBody
    public String markNotificationsAsRead(Principal principal) {
        memberNotificationService.markAllNotificationsAsRead(principal.getName());
        return "OK";
    }

    @PostMapping("/notifications/{notificationId}/mark-read")
    @ResponseBody
    public Map<String, Long> markNotificationAsRead(@PathVariable Integer notificationId,
                                                     Principal principal) {
        return Map.of("unreadCount",
                memberNotificationService.markNotificationAsRead(principal.getName(), notificationId));
    }

    @GetMapping("/reviews")
    public String showReviewForm(Model model,
                                 Principal principal,
                                 @RequestParam(defaultValue = "0") int page,
                                 @RequestParam(required = false) Integer editReviewId) {
        if (!model.containsAttribute("reviewRequest")) {
            model.addAttribute("reviewRequest", new MemberReviewSubmitRequest());
        }
        if (editReviewId != null && !model.containsAttribute("reviewEditRequest")) {
            try {
                model.addAttribute("reviewEditRequest",
                        memberReviewService.getMyReviewForEdit(principal.getName(), editReviewId));
            } catch (ApplicationException e) {
                model.addAttribute("error", e.getMessage());
                editReviewId = null;
            }
        }
        model.addAttribute("editReviewId", editReviewId);
        model.addAttribute("books", bookRepository.findAll());
        model.addAttribute("myReviews", memberReviewService.getMyReviews(
                principal.getName(), PageRequest.of(Math.max(0, page), PAGE_SIZE)));

        return "member/reviews";
    }

    @PostMapping("/reviews")
    public String submitReview(@Valid @ModelAttribute("reviewRequest") MemberReviewSubmitRequest request,
                               BindingResult bindingResult,
                               Model model,
                               Principal principal,
                               RedirectAttributes flash) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("books", bookRepository.findAll());
            model.addAttribute("myReviews", memberReviewService.getMyReviews(
                    principal.getName(), PageRequest.of(0, PAGE_SIZE)));
            return "member/reviews";
        }

        try {
            memberReviewService.submitReview(principal.getName(), request);
            flash.addFlashAttribute("success",
                    message("backend.review.submitted"));
            return "redirect:/member/interaction/reviews";
        } catch (ApplicationException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("books", bookRepository.findAll());
            model.addAttribute("myReviews", memberReviewService.getMyReviews(
                    principal.getName(), PageRequest.of(0, PAGE_SIZE)));
            return "member/reviews";
        }
    }

    @PostMapping("/books/{bookId}/reviews")
    public String submitBookDetailReview(@PathVariable("bookId") Integer bookId,
                                         @Valid @ModelAttribute("reviewRequest") MemberReviewSubmitRequest request,
                                         BindingResult bindingResult,
                                         Principal principal,
                                         RedirectAttributes flash) {
        request.setBookId(bookId);

        if (bindingResult.hasErrors()) {
            String message = bindingResult.getAllErrors().isEmpty()
                    ? message("backend.review.checkContent")
                    : bindingResult.getAllErrors().get(0).getDefaultMessage();
            flash.addFlashAttribute("error", message);
            flash.addFlashAttribute("reviewRequest", request);
            return "redirect:/books/" + bookId;
        }

        try {
            memberReviewService.submitReview(principal.getName(), request);
            flash.addFlashAttribute("reviewSubmittedSuccess", true);
        } catch (ApplicationException e) {
            flash.addFlashAttribute("error", e.getMessage());
            flash.addFlashAttribute("reviewRequest", request);
        }

        return "redirect:/books/" + bookId;
    }

    @PostMapping("/reviews/{feedbackId}/edit")
    public String updateMyReview(@PathVariable("feedbackId") Integer feedbackId,
                                 @Valid @ModelAttribute("reviewEditRequest") MemberReviewUpdateRequest request,
                                 BindingResult bindingResult,
                                 @RequestParam(defaultValue = "0") int page,
                                 Principal principal,
                                 RedirectAttributes flash) {
        int safePage = Math.max(0, page);

        if (bindingResult.hasErrors()) {
            String message = bindingResult.getAllErrors().isEmpty()
                    ? message("backend.review.checkContent")
                    : bindingResult.getAllErrors().get(0).getDefaultMessage();
            flash.addFlashAttribute("error", message);
            flash.addFlashAttribute("reviewEditRequest", request);
            flash.addAttribute("page", safePage);
            flash.addAttribute("editReviewId", feedbackId);
            return "redirect:/member/interaction/reviews";
        }

        try {
            memberReviewService.updateMyReview(principal.getName(), feedbackId, request);
            flash.addFlashAttribute("success", message("backend.review.updated"));
        } catch (ApplicationException e) {
            flash.addFlashAttribute("error", e.getMessage());
            flash.addFlashAttribute("reviewEditRequest", request);
            flash.addAttribute("editReviewId", feedbackId);
        }

        flash.addAttribute("page", safePage);
        return "redirect:/member/interaction/reviews";
    }

    @PostMapping("/reviews/{feedbackId}/delete")
    public String deleteMyReview(@PathVariable("feedbackId") Integer feedbackId,
                                 Principal principal,
                                 RedirectAttributes flash) {
        try {
            memberReviewService.deleteMyReview(principal.getName(), feedbackId);
            flash.addFlashAttribute("success", message("backend.review.deleted"));
        } catch (ApplicationException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/member/interaction/reviews";
    }

    @GetMapping("/acquisition-requests/new")
    public String showBookAcquisitionRequestForm(Model model,
                                                 Principal principal,
                                                 @RequestParam(defaultValue = "0") int page) {
        if (!model.containsAttribute("acquisitionRequest")) {
            model.addAttribute("acquisitionRequest", new MemberBookAcquisitionRequest());
        }
        model.addAttribute("myAcquisitionRequests", memberBookAcquisitionService.getMyRequests(
                principal.getName(), PageRequest.of(Math.max(0, page), PAGE_SIZE)));

        return "member/book-acquisition-request";
    }

    @PostMapping("/acquisition-requests")
    public String submitBookAcquisitionRequest(
            @Valid @ModelAttribute("acquisitionRequest") MemberBookAcquisitionRequest request,
            BindingResult bindingResult,
            Model model,
            Principal principal,
            RedirectAttributes flash) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("myAcquisitionRequests", memberBookAcquisitionService.getMyRequests(
                    principal.getName(), PageRequest.of(0, PAGE_SIZE)));
            return "member/book-acquisition-request";
        }

        try {
            memberBookAcquisitionService.submitRequest(principal.getName(), request);
            flash.addFlashAttribute("success", message("backend.acquisition.submitted"));
            return "redirect:/member/interaction/acquisition-requests/new";

        } catch (ApplicationException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("myAcquisitionRequests", memberBookAcquisitionService.getMyRequests(
                    principal.getName(), PageRequest.of(0, PAGE_SIZE)));
            return "member/book-acquisition-request";
        }
    }

    @GetMapping("/favorites")
    public String redirectToCanonicalFavorites() {
        return "redirect:/member/favorites";
    }

    @PostMapping("/favorites/{bookId}/add")
    public Object addToFavorites(
            @PathVariable("bookId") Integer bookId,
            Principal principal,
            RedirectAttributes flash,
            @RequestHeader(value = "X-Requested-With", required = false) String requestedWith,
            @RequestHeader(value = "Referer", required = false) String referer) {

        try {
            memberFavoriteService.addToFavorites(principal.getName(), bookId);
            if (isAjax(requestedWith)) {
                return org.springframework.http.ResponseEntity.ok(favoriteJson(true, message("backend.favorite.added")));
            }
            flash.addFlashAttribute("success", message("backend.favorite.added"));
        } catch (ApplicationException e) {
            if (isAjax(requestedWith)) {
                return org.springframework.http.ResponseEntity
                        .status(e.getStatus())
                        .body(favoriteJson(false, e.getMessage()));
            }
            flash.addFlashAttribute("error", e.getMessage());
        }

        return org.springframework.http.ResponseEntity
                .status(org.springframework.http.HttpStatus.SEE_OTHER)
                .location(URI.create(localRedirectTarget(referer)))
                .build();
    }

    @PostMapping("/favorites/{bookId}/remove")
    public String removeFromFavorites(
            @PathVariable("bookId") Integer bookId,
            Principal principal,
            RedirectAttributes flash) {

        try {
            memberFavoriteService.removeFromFavorites(principal.getName(), bookId);
            flash.addFlashAttribute("success", message("backend.favorite.removed"));
        } catch (ApplicationException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/member/favorites";
    }

    private boolean isAjax(String requestedWith) {
        return "XMLHttpRequest".equalsIgnoreCase(requestedWith);
    }

    private String localRedirectTarget(String referer) {
        if (referer == null || referer.isBlank()) {
            return "/books";
        }
        try {
            URI uri = URI.create(referer);
            String path = uri.getPath();
            if (path == null || !path.startsWith("/") || path.startsWith("//")) {
                return "/books";
            }
            StringBuilder target = new StringBuilder(path);
            if (uri.getQuery() != null) target.append('?').append(uri.getQuery());
            if (uri.getFragment() != null) target.append('#').append(uri.getFragment());
            return target.toString();
        } catch (IllegalArgumentException exception) {
            return "/books";
        }
    }

    private NotificationSource parseNotificationSource(String value) {
        if (value == null || value.isBlank() || "all".equalsIgnoreCase(value)) {
            return null;
        }
        try {
            return NotificationSource.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private NotificationType parseNotificationType(String value) {
        if (value == null || value.isBlank() || "ALL".equalsIgnoreCase(value)) {
            return null;
        }
        try {
            return NotificationType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private Map<String, Object> favoriteJson(boolean favorite, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", favorite);
        body.put("favorite", favorite);
        body.put("message", message);
        return body;
    }
}
