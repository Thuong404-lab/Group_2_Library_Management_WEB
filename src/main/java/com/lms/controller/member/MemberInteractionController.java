package com.lms.controller.member;

import com.lms.repository.BookRepository;
import com.lms.service.MemberNotificationService;
import com.lms.service.MemberReviewService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import java.security.Principal;
import com.lms.dto.request.MemberReviewSubmitRequest;
import com.lms.exception.ResourceNotFoundException;
import com.lms.exception.ValidationException;
import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.lms.dto.request.MemberBookAcquisitionRequest;
import com.lms.service.MemberBookAcquisitionService;

@Controller
@RequestMapping("/member/interaction")
public class MemberInteractionController {

    private final MemberNotificationService memberNotificationService;
    private final MemberReviewService memberReviewService;
    private final BookRepository bookRepository;
    private final MemberBookAcquisitionService memberBookAcquisitionService;

    public MemberInteractionController(MemberNotificationService memberNotificationService,
                                       MemberReviewService memberReviewService,
                                       BookRepository bookRepository,
                                       MemberBookAcquisitionService memberBookAcquisitionService) {
        this.memberNotificationService = memberNotificationService;
        this.memberReviewService = memberReviewService;
        this.bookRepository = bookRepository;
        this.memberBookAcquisitionService = memberBookAcquisitionService;
    }

    @GetMapping("/notifications")
    public String viewNotifications(Model model, Principal principal) {
        model.addAttribute(
                "notifications",
                memberNotificationService.getMyNotifications(principal.getName())
        );
        model.addAttribute("showNotificationBell", false);

        return "member/notifications";
    }

    @GetMapping("/reviews")
    public String showReviewForm(Model model) {
        if (!model.containsAttribute("reviewRequest")) {
            model.addAttribute("reviewRequest", new MemberReviewSubmitRequest());
        }
        model.addAttribute("books", bookRepository.findAll());

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
            return "member/reviews";
        }
    }

    @GetMapping("/acquisition-requests/new")
    public String showBookAcquisitionRequestForm(Model model) {
        if (!model.containsAttribute("acquisitionRequest")) {
            model.addAttribute("acquisitionRequest", new MemberBookAcquisitionRequest());
        }

        return "member/member-book-acquisition-request";
    }

    @PostMapping("/acquisition-requests")
    public String submitBookAcquisitionRequest(
            @Valid @ModelAttribute("acquisitionRequest") MemberBookAcquisitionRequest request,
            BindingResult bindingResult,
            Model model,
            Principal principal,
            RedirectAttributes flash) {

        if (bindingResult.hasErrors()) {
            return "member/member-book-acquisition-request";
        }

        try {
            memberBookAcquisitionService.submitRequest(principal.getName(), request);
            flash.addFlashAttribute("success", "Đã gửi đề xuất sách thành công.");
            return "redirect:/member/interaction/acquisition-requests/new";

        } catch (ValidationException | ResourceNotFoundException e) {
            model.addAttribute("error", e.getMessage());
            return "member/member-book-acquisition-request";
        }
    }
}