package com.lms.controller.admin;
import com.lms.exception.ApplicationException;

import com.lms.config.CustomUserDetails;
import com.lms.dto.request.AdminAccountCreateRequest;
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
@RequestMapping("/admin/accounts")
public class AccountController {

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

        return "admin/accounts";
    }

    @GetMapping("/search")
    public String searchAccounts(@RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        return listAccounts(page, keyword, "", "", model);
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

    @GetMapping("/create/validate")
    @ResponseBody
    public Map<String, String> validateAccountCreate(
            @RequestParam(required = false, defaultValue = "") String fullName,
            @RequestParam(required = false, defaultValue = "") String email,
            @RequestParam(required = false, defaultValue = "") String phone,
            @RequestParam(required = false, defaultValue = "") String username,
            @RequestParam(required = false, defaultValue = "") String password,
            @RequestParam(required = false, defaultValue = "") String accountType,
            @RequestParam(required = false) Integer tierId,
            @RequestParam(required = false, defaultValue = "Active") String status) {
        return accountService.validateAccountCreate(new AdminAccountCreateRequest(
                fullName, email, phone, username, password, accountType, tierId, status));
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
            @AuthenticationPrincipal CustomUserDetails currentUser,
            RedirectAttributes redirectAttributes) {
        try {
            accountService.deleteAccount(id, source, accountIdOf(currentUser));
            redirectAttributes.addFlashAttribute("success", "Xóa tài khoản thành công.");
        } catch (AccountFormValidationException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return redirectBySource(source);
    }

    @PostMapping("/{id}/send-password-reset")
    public String sendPasswordReset(@PathVariable Integer id,
            RedirectAttributes redirectAttributes) {
        try {
            String email = accountService.getMemberEmail(id);
            authService.requestPasswordReset(email);
            redirectAttributes.addFlashAttribute("success",
                    "Đã gửi liên kết đặt lại mật khẩu đến email " + email + ".");
        } catch (ApplicationException e) {
            redirectAttributes.addFlashAttribute("error",
                    e.getMessage() == null || e.getMessage().isBlank()
                            ? "Không thể gửi liên kết đặt lại mật khẩu. Vui lòng thử lại."
                            : e.getMessage());
        }
        return "redirect:/admin/accounts";
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

    private Integer accountIdOf(CustomUserDetails currentUser) {
        return currentUser == null ? null : currentUser.getAccountId();
    }
}
