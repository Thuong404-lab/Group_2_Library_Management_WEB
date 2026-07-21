package com.lms.controller.admin;

import com.lms.controller.LocalizedControllerSupport;
import com.lms.dto.request.AdminStaffAccountCreateRequest;
import com.lms.exception.AccountFormValidationException;
import com.lms.service.AccountService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/admin/staff")
public class StaffAccountController extends LocalizedControllerSupport {

    private final AccountService accountService;

    public StaffAccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/create/validate")
    @ResponseBody
    public Map<String, String> validateStaffAccountCreate(
            @RequestParam(required = false, defaultValue = "") String fullName,
            @RequestParam(required = false, defaultValue = "") String email,
            @RequestParam(required = false, defaultValue = "") String phone,
            @RequestParam(required = false, defaultValue = "") String username,
            @RequestParam(required = false, defaultValue = "") String password,
            @RequestParam(required = false, defaultValue = "") String staffType) {
        AdminStaffAccountCreateRequest request = new AdminStaffAccountCreateRequest(
                fullName, email, phone, username, password, staffType);
        return accountService.validateStaffAccountCreate(request);
    }

    @PostMapping("/create")
    public String createStaffAccount(@RequestParam String fullName,
            @RequestParam String email,
            @RequestParam(required = false, defaultValue = "") String phone,
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String staffType,
            RedirectAttributes redirectAttributes) {
        AdminStaffAccountCreateRequest request = new AdminStaffAccountCreateRequest(
                fullName, email, phone, username, password, staffType);
        try {
            accountService.createStaffAccount(request);
            redirectAttributes.addFlashAttribute("success", message("backend.account.created"));
        } catch (AccountFormValidationException exception) {
            redirectAttributes.addFlashAttribute("formValues", createFormValues(request));
            redirectAttributes.addFlashAttribute("fieldErrors", exception.getFieldErrors());
            redirectAttributes.addFlashAttribute("openCreateAccountModal", true);
        }
        return "redirect:/admin/staff";
    }

    private Map<String, Object> createFormValues(AdminStaffAccountCreateRequest request) {
        Map<String, Object> formValues = new HashMap<>();
        formValues.put("fullName", trim(request.getFullName()));
        formValues.put("email", trim(request.getEmail()));
        formValues.put("phone", trim(request.getPhone()));
        formValues.put("username", trim(request.getUsername()));
        formValues.put("staffType", trim(request.getStaffType()));
        return formValues;
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
