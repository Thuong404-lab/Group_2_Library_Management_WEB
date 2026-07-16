package com.lms.controller.member;

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
import com.lms.exception.ResourceNotFoundException;
import com.lms.exception.ValidationException;
import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.lms.dto.request.MemberBookAcquisitionRequest;
import com.lms.service.MemberBookAcquisitionService;

@Controller
@RequestMapping("/member/interaction")
public class MemberInteractionController {

    private static final int PAGE_SIZE = 10;

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
                                    @RequestParam(defaultValue = "0") int page) {
        memberNotificationService.markAllNotificationsAsRead(principal.getName());

        model.addAttribute(
                "notifications",
                memberNotificationService.getMyNotifications(
                        principal.getName(), PageRequest.of(Math.max(0, page), PAGE_SIZE))
        );
        model.addAttribute("showNotificationBell", false);

        return "member/notifications";
    }

    @GetMapping("/notifications/mark-read")
    @ResponseBody
    public String markNotificationsAsRead(Principal principal) {
        memberNotificationService.markAllNotificationsAsRead(principal.getName());
        return "OK";
    }

    @GetMapping("/reviews")
    public String showReviewForm(Model model,
                                 Principal principal,
                                 @RequestParam(defaultValue = "0") int page) {
        if (!model.containsAttribute("reviewRequest")) {
            model.addAttribute("reviewRequest", new MemberReviewSubmitRequest());
        }
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
                    "Đã gửi đánh giá thành công.");
            return "redirect:/member/interaction/reviews";
        } catch (ValidationException | ResourceNotFoundException e) {
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
                    ? "Vui lòng kiểm tra lại nội dung đánh giá."
                    : bindingResult.getAllErrors().get(0).getDefaultMessage();
            flash.addFlashAttribute("error", message);
            flash.addFlashAttribute("reviewRequest", request);
            return "redirect:/books/" + bookId;
        }

        try {
            memberReviewService.submitReview(principal.getName(), request);
            flash.addFlashAttribute("success", "Đã gửi đánh giá thành công.");
        } catch (ValidationException | ResourceNotFoundException e) {
            flash.addFlashAttribute("error", e.getMessage());
            flash.addFlashAttribute("reviewRequest", request);
        }

        return "redirect:/books/" + bookId;
    }

    @PostMapping("/reviews/{feedbackId}/delete")
    public String deleteMyReview(@PathVariable("feedbackId") Integer feedbackId,
                                 Principal principal,
                                 RedirectAttributes flash) {
        try {
            memberReviewService.deleteMyReview(principal.getName(), feedbackId);
            flash.addFlashAttribute("success", "Đã xoá đánh giá thành công.");
        } catch (ValidationException | ResourceNotFoundException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/member/interaction/reviews";
    }

    @GetMapping("/acquisition-requests/new")
    public String showBookAcquisitionRequestForm(Model model) {
        if (!model.containsAttribute("acquisitionRequest")) {
            model.addAttribute("acquisitionRequest", new MemberBookAcquisitionRequest());
        }

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
            return "member/book-acquisition-request";
        }

        try {
            memberBookAcquisitionService.submitRequest(principal.getName(), request);
            flash.addFlashAttribute("success", "Đã gửi đề xuất sách thành công.");
            return "redirect:/member/interaction/acquisition-requests/new";

        } catch (ValidationException | ResourceNotFoundException e) {
            model.addAttribute("error", e.getMessage());
            return "member/book-acquisition-request";
        }
    }

    @GetMapping("/favorites")
    public String viewFavorites(Model model,
                                Principal principal,
                                @RequestParam(defaultValue = "0") int page) {
        model.addAttribute("favorites", memberFavoriteService.getMyFavorites(
                principal.getName(), PageRequest.of(Math.max(0, page), PAGE_SIZE)));

        return "member/favorites";
    }

    @PostMapping("/favorites/{bookId}/add")
    public Object addToFavorites(
            @PathVariable("bookId") Integer bookId,
            Principal principal,
            RedirectAttributes flash,
            @RequestHeader(value = "X-Requested-With", required = false) String requestedWith,
            @RequestHeader(value = "Referer", required = false) String referer) {

        System.out.println(">>> ĐÃ VÀO CONTROLLER ADD FAVORITE: BookID = " + bookId);

        try {
            memberFavoriteService.addToFavorites(principal.getName(), bookId);
            if (isAjax(requestedWith)) {
                return org.springframework.http.ResponseEntity.ok(favoriteJson(true, "Đã thêm vào danh sách yêu thích!"));
            }
            flash.addFlashAttribute("success", "Đã thêm vào yêu thích!");
        } catch (ValidationException e) {
            e.printStackTrace();
            if (isAjax(requestedWith)) {
                return org.springframework.http.ResponseEntity.ok(favoriteJson(true, e.getMessage()));
            }
            flash.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            if (isAjax(requestedWith)) {
                return org.springframework.http.ResponseEntity
                        .badRequest()
                        .body(favoriteJson(false, e.getMessage()));
            }
            flash.addFlashAttribute("error", e.getMessage());
        }

        return org.springframework.http.ResponseEntity
                .status(org.springframework.http.HttpStatus.SEE_OTHER)
                .location(URI.create(referer != null && !referer.isBlank() ? referer : "/books"))
                .build();
    }

    @PostMapping("/favorites/{bookId}/remove")
    public String removeFromFavorites(
            @PathVariable("bookId") Integer bookId,
            Principal principal,
            RedirectAttributes flash) {

        try {
            memberFavoriteService.removeFromFavorites(principal.getName(), bookId);
            flash.addFlashAttribute("success", "Đã bỏ sách khỏi danh sách yêu thích!");
        } catch (Exception e) {
            e.printStackTrace();
            flash.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/member/favorites";
    }

    private boolean isAjax(String requestedWith) {
        return "XMLHttpRequest".equalsIgnoreCase(requestedWith);
    }

    private Map<String, Object> favoriteJson(boolean favorite, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", favorite);
        body.put("favorite", favorite);
        body.put("message", message);
        return body;
    }
}
