package com.lms.controller.admin;

import com.lms.exception.ApplicationException;

import com.lms.config.CustomUserDetails;
import com.lms.controller.LocalizedControllerSupport;
import com.lms.dto.request.AdminMemberAccountCreateRequest;
import com.lms.dto.request.AdminAccountUpdateRequest;
import com.lms.dto.response.AdminAccountListViewData;
import com.lms.exception.AccountFormValidationException;
import com.lms.service.AccountService;
import com.lms.service.AuthService;
import org.springframework.stereotype.Controller;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
@RequestMapping("/admin/member-list")
public class AccountController extends LocalizedControllerSupport {

    private final AccountService accountService;
    private final AuthService authService;

    public AccountController(AccountService accountService, AuthService authService) {
        this.accountService = accountService;
        this.authService = authService;
    }

    @GetMapping
    public String listAccounts(@RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(required = false, defaultValue = "") String status,
            @RequestParam(required = false, defaultValue = "") String tier,
            Model model) {
        AdminAccountListViewData data = accountService.getMemberAccountList(page, keyword, status, tier);

        model.addAttribute("accounts", data.accounts());
        model.addAttribute("keyword", keyword);
        model.addAttribute("memberByUserId", data.memberByUserId());
        model.addAttribute("tiers", data.tiers());
        model.addAttribute("memberSummary", data.summaryCounts());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedTier", tier);

        return "admin/member-list";
    }

    @GetMapping("/search")
    public String searchAccounts(@RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        return listAccounts(page, keyword, "", "", model);
    }

    @GetMapping("/create/validate")
    @ResponseBody
    public Map<String, String> validateMemberAccountCreate(
            @RequestParam(required = false, defaultValue = "") String fullName,
            @RequestParam(required = false, defaultValue = "") String email,
            @RequestParam(required = false, defaultValue = "") String phone,
            @RequestParam(required = false, defaultValue = "") String username,
            @RequestParam(required = false, defaultValue = "") String password) {
        AdminMemberAccountCreateRequest request = new AdminMemberAccountCreateRequest(
                fullName, email, phone, username, password);
        return accountService.validateMemberAccountCreate(request);
    }

    @PostMapping("/create")
    public String createMemberAccount(@RequestParam String fullName,
            @RequestParam String email,
            @RequestParam(required = false, defaultValue = "") String phone,
            @RequestParam String username,
            @RequestParam String password,
            RedirectAttributes redirectAttributes) {

        AdminMemberAccountCreateRequest request = new AdminMemberAccountCreateRequest(
                fullName, email, phone, username, password);
        try {
            accountService.createMemberAccount(request);
            redirectAttributes.addFlashAttribute("success", message("backend.account.created"));
            return "redirect:/admin/member-list";
        } catch (AccountFormValidationException e) {
            redirectAttributes.addFlashAttribute("formValues", createFormValues(request));
            redirectAttributes.addFlashAttribute("fieldErrors", e.getFieldErrors());
            redirectAttributes.addFlashAttribute("openCreateAccountModal", true);
            return "redirect:/admin/member-list";
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
            @RequestParam(required = false, defaultValue = "members") String source,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        AdminAccountUpdateRequest request = new AdminAccountUpdateRequest(
                id, fullName, email, phone, username, tierId, staffType, status, source);
        return accountService.validateAccountUpdate(request, accountIdOf(currentUser));
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
            @AuthenticationPrincipal CustomUserDetails currentUser,
            RedirectAttributes redirectAttributes) {

        AdminAccountUpdateRequest request = new AdminAccountUpdateRequest(
                id, fullName, email, phone, username, tierId, staffType, status, source);

        try {
            accountService.updateAccount(request, accountIdOf(currentUser));
            redirectAttributes.addFlashAttribute("success", message("backend.account.updated"));
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
            @AuthenticationPrincipal CustomUserDetails currentUser,
            RedirectAttributes redirectAttributes) {
        try {
            accountService.deleteAccount(id, source, accountIdOf(currentUser));
            redirectAttributes.addFlashAttribute("success",
                    message("staff".equalsIgnoreCase(source)
                            ? "backend.account.deactivated"
                            : "backend.account.deleted"));
        } catch (AccountFormValidationException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return redirectBySource(source);
    }

    @PostMapping("/{id}/send-password-reset")
    public String sendPasswordReset(@PathVariable Integer id,
            @RequestParam(required = false, defaultValue = "members") String source,
            RedirectAttributes redirectAttributes) {
        try {
            String email = "staff".equalsIgnoreCase(source)
                    ? accountService.getStaffEmail(id)
                    : accountService.getMemberEmail(id);
            authService.requestPasswordReset(email);
            redirectAttributes.addFlashAttribute("success", message("backend.account.resetSent", email));
        } catch (ApplicationException e) {
            redirectAttributes.addFlashAttribute("error",
                    e.getMessage() == null || e.getMessage().isBlank()
                            ? message("backend.account.resetFailed")
                            : e.getMessage());
        }
        return redirectBySource(source);
    }

    private Map<String, Object> createFormValues(AdminMemberAccountCreateRequest request) {
        Map<String, Object> formValues = new HashMap<>();
        formValues.put("fullName", trim(request.getFullName()));
        formValues.put("email", trim(request.getEmail()));
        formValues.put("phone", trim(request.getPhone()));
        formValues.put("username", trim(request.getUsername()));
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

        return "redirect:/admin/member-list";
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private Integer accountIdOf(CustomUserDetails currentUser) {
        return currentUser == null ? null : currentUser.getAccountId();
    }
}
