package com.lms.controller.admin;

import com.lms.dto.request.AdminAccountCreateRequest;
import com.lms.dto.request.AdminAccountUpdateRequest;
import com.lms.entity.Member;
import com.lms.entity.MemberAccount;
import com.lms.exception.AccountFormValidationException;
import com.lms.service.AccountService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/admin/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    public String listAccounts(@RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "") String keyword,
            Model model) {
        PageRequest pageRequest = PageRequest.of(page, 10, Sort.by("id").ascending());
        Page<MemberAccount> accounts = accountService.getMemberAccounts(keyword, pageRequest);
        Map<Integer, Member> memberByUserId = accountService.getMemberByUserId(accounts);

        model.addAttribute("accounts", accounts);
        model.addAttribute("keyword", keyword);
        model.addAttribute("memberByUserId", memberByUserId);
        model.addAttribute("tiers", accountService.getMembershipTiers());

        return "admin/accounts";
    }

    @GetMapping("/search")
    public String searchAccounts(@RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        return listAccounts(page, keyword, model);
    }

    @PostMapping("/create")
    public String createAccount(@RequestParam String fullName,
            @RequestParam String email,
            @RequestParam(required = false, defaultValue = "") String phone,
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String accountType,
            @RequestParam(required = false) Integer tierId,
            @RequestParam(defaultValue = "Active") String status,
            @RequestParam(required = false, defaultValue = "members") String source,
            Model model,
            RedirectAttributes redirectAttributes) {

        AdminAccountCreateRequest request = new AdminAccountCreateRequest(
                fullName, email, phone, username, password, accountType, tierId, status);
        try {
            accountService.createAccount(request);
            redirectAttributes.addFlashAttribute("success", "Tạo tài khoản thành công.");
            return redirectBySource(source);
        } catch (AccountFormValidationException e) {
            redirectAttributes.addFlashAttribute("formValues", createFormValues(request));
            redirectAttributes.addFlashAttribute("fieldErrors", e.getFieldErrors());
            redirectAttributes.addFlashAttribute("openCreateAccountModal", true);
            return redirectBySource(source);
        }
    }

    @GetMapping("/edit/{id}/validate")
    @ResponseBody
    public Map<String, String> validateAccountUpdate(
            @PathVariable Integer id,
            @RequestParam(required = false, defaultValue = "") String fullName,
            @RequestParam(required = false, defaultValue = "") String email,
            @RequestParam(required = false, defaultValue = "") String phone,
            @RequestParam(required = false, defaultValue = "") String username,
            @RequestParam(required = false) Integer tierId,
            @RequestParam(required = false) String staffType,
            @RequestParam(required = false, defaultValue = "") String status,
            @RequestParam(required = false, defaultValue = "members") String source) {
        AdminAccountUpdateRequest request = new AdminAccountUpdateRequest(
                id, fullName, email, phone, username, tierId, staffType, status, source);
        return accountService.validateAccountUpdate(request);
    }

    @PostMapping("/edit/{id}")
    public String updateAccount(@PathVariable Integer id,
            @RequestParam String fullName,
            @RequestParam String email,
            @RequestParam(required = false, defaultValue = "") String phone,
            @RequestParam String username,
            @RequestParam(required = false) Integer tierId,
            @RequestParam(required = false) String staffType,
            @RequestParam(defaultValue = "Active") String status,
            @RequestParam(required = false, defaultValue = "members") String source,
            RedirectAttributes redirectAttributes) {

        AdminAccountUpdateRequest request = new AdminAccountUpdateRequest(
                id, fullName, email, phone, username, tierId, staffType, status, source);

        try {
            accountService.updateAccount(request);
            redirectAttributes.addFlashAttribute("success", "Cập nhật tài khoản thành công.");
            return redirectBySource(source);
        } catch (AccountFormValidationException e) {
            redirectAttributes.addFlashAttribute("editAccountId", id);
            redirectAttributes.addFlashAttribute("editFormValues", createEditFormValues(request));
            redirectAttributes.addFlashAttribute("editFieldErrors", e.getFieldErrors());
            String modalId = "staff".equalsIgnoreCase(source)
                    ? "updateStaffModal" + id
                    : "updateMemberModal" + id;
            return redirectBySource(source) + "#" + modalId;
        }
    }

    @PostMapping("/delete/{id}")
    public String deleteAccount(@PathVariable Integer id,
            @RequestParam(required = false, defaultValue = "members") String source,
            RedirectAttributes redirectAttributes) {
        try {
            accountService.deleteAccount(id, source);
            redirectAttributes.addFlashAttribute("success", "Xóa tài khoản thành công.");
        } catch (AccountFormValidationException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return redirectBySource(source);
    }

    private Map<String, Object> createFormValues(AdminAccountCreateRequest request) {
        Map<String, Object> formValues = new HashMap<>();
        formValues.put("fullName", trim(request.getFullName()));
        formValues.put("email", trim(request.getEmail()));
        formValues.put("phone", trim(request.getPhone()));
        formValues.put("username", trim(request.getUsername()));
        formValues.put("accountType", trim(request.getAccountType()).toUpperCase());
        formValues.put("tierId", request.getTierId());
        formValues.put("status", request.getStatus());
        return formValues;
    }

    private Map<String, Object> createEditFormValues(AdminAccountUpdateRequest request) {
        Map<String, Object> editFormValues = new HashMap<>();
        editFormValues.put("fullName", trim(request.getFullName()));
        editFormValues.put("username", trim(request.getUsername()));
        editFormValues.put("email", trim(request.getEmail()));
        editFormValues.put("phone", trim(request.getPhone()));
        editFormValues.put("tierId", request.getTierId());
        editFormValues.put("staffType", request.getStaffType());
        editFormValues.put("status", request.getStatus());
        return editFormValues;
    }

    private String redirectBySource(String source) {
        if ("staff".equalsIgnoreCase(source)) {
            return "redirect:/admin/staff";
        }

        return "redirect:/admin/accounts";
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
