package com.lms.controller.librarian;

import com.lms.config.CustomUserDetails;
import com.lms.dto.request.CreateMemberAccountRequest;
import com.lms.dto.request.UpdateMemberAccountRequest;
import com.lms.dto.response.MemberListViewData;
import com.lms.service.LibrarianMemberService;
import jakarta.validation.Valid;
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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@RequestMapping("/librarian")
public class MemberMgmtController {

    private final LibrarianMemberService memberService;

    public MemberMgmtController(LibrarianMemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping("/members")
    public String viewMemberList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "") String keyword,
            Model model,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        MemberListViewData data = memberService.getMemberList(page, keyword);
        model.addAttribute("accounts", data.accounts());
        model.addAttribute("memberByUserId", data.memberByUserId());
        model.addAttribute("tiers", data.tiers());
        model.addAttribute("keyword", keyword);
        addCurrentUser(model, userDetails);
        return "librarian/member-list";
    }

    @GetMapping("/members/create")
    public String showCreateMemberForm(
            Model model,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        model.addAttribute("tiers", memberService.getMembershipTiers());
        addCurrentUser(model, userDetails);
        return "librarian/create-member";
    }

    @PostMapping("/members/create")
    public String createMemberAccount(
            @Valid @ModelAttribute CreateMemberAccountRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Map<String, String> errors = bindingErrors(bindingResult);
        mergeErrors(errors, memberService.validateCreate(request));
        if (!errors.isEmpty()) {
            model.addAttribute("fieldErrors", errors);
            model.addAttribute("formValues", createFormValues(request));
            model.addAttribute("tiers", memberService.getMembershipTiers());
            addCurrentUser(model, userDetails);
            return "librarian/create-member";
        }

        memberService.createMember(request);
        redirectAttributes.addFlashAttribute(
                "success", "Tạo tài khoản thành viên thành công.");
        return "redirect:/librarian/members";
    }

    @PostMapping("/members/edit/{id}")
    public String updateMemberAccount(
            @PathVariable Integer id,
            @Valid @ModelAttribute UpdateMemberAccountRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        Map<String, String> errors = bindingErrors(bindingResult);
        mergeErrors(errors, memberService.validateUpdate(id, request));
        if (!errors.isEmpty()) {
            redirectAttributes.addFlashAttribute(
                    "error", errors.values().iterator().next());
            return "redirect:/librarian/members";
        }

        memberService.updateMember(id, request);
        redirectAttributes.addFlashAttribute(
                "success", "Cập nhật tài khoản thành viên thành công.");
        return "redirect:/librarian/members";
    }

    @GetMapping("/members/edit/{id}/validate")
    @ResponseBody
    public Map<String, String> validateMemberUpdateFields(
            @PathVariable Integer id,
            @Valid @ModelAttribute UpdateMemberAccountRequest request,
            BindingResult bindingResult) {

        Map<String, String> errors = bindingErrors(bindingResult);
        mergeErrors(errors, memberService.validateUpdate(id, request));
        return errors;
    }

    @PostMapping("/members/delete/{id}")
    public String deleteMemberAccount(
            @PathVariable Integer id,
            RedirectAttributes redirectAttributes) {
        if (!memberService.deactivateMember(id)) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy tài khoản.");
        } else {
            redirectAttributes.addFlashAttribute(
                    "success", "Xóa tài khoản thành viên thành công.");
        }
        return "redirect:/librarian/members";
    }

    @GetMapping("/members/fines")
    public String manageFines() {
        return "librarian/fines";
    }

    @PostMapping("/members/fines/create")
    public String createFine(
            @RequestParam Integer memberId,
            @RequestParam Double amount,
            @RequestParam String reason) {
        return "redirect:/librarian/members/fines?created";
    }

    @GetMapping("/members/transactions")
    public String viewAllTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String type) {
        return "librarian/transactions";
    }

    @GetMapping("/members/topup")
    public String showTopupDesk() {
        return "librarian/topup-desk";
    }

    @PostMapping("/members/topup")
    public String topUpMemberAccount(
            @RequestParam String memberPhone,
            @RequestParam Double amount) {
        return "redirect:/librarian/members/topup?success";
    }

    private Map<String, String> bindingErrors(BindingResult bindingResult) {
        Map<String, String> errors = new LinkedHashMap<>();
        bindingResult.getFieldErrors().forEach(error ->
                errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return errors;
    }

    private void mergeErrors(Map<String, String> target, Map<String, String> source) {
        source.forEach(target::putIfAbsent);
    }

    private Map<String, Object> createFormValues(CreateMemberAccountRequest request) {
        Map<String, Object> values = new HashMap<>();
        values.put("fullName", trim(request.getFullName()));
        values.put("email", trim(request.getEmail()));
        values.put("phone", trim(request.getPhone()));
        values.put("username", trim(request.getUsername()));
        values.put("tierId", request.getTierId());
        values.put("status", request.getStatus());
        return values;
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private void addCurrentUser(Model model, CustomUserDetails userDetails) {
        if (userDetails != null && userDetails.getAccount() != null) {
            model.addAttribute("currentUser", userDetails.getAccount().getUser());
        }
    }
}
