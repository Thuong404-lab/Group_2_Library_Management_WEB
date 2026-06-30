package com.lms.controller.admin;

import com.lms.entity.User;
import com.lms.service.ProfileService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

/**
 * AdminProfileController - Chỉ dành riêng cho vai trò ADMIN
 */
@Controller
@RequestMapping("/admin/profile")
public class AdminProfileController {

    private final ProfileService profileService;

    public AdminProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public String viewAdminProfile(Principal principal, Model model) {
        if (principal == null)
            return "redirect:/login";
        String username = principal.getName();
        User admin = profileService.getProfile(username);
        model.addAttribute("admin", admin);
        return "admin/profile";
    }

    @PostMapping("/update")
    public String updateProfile(@RequestParam String fullName,
            @RequestParam String email,
            @RequestParam String phone,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        if (principal == null)
            return "redirect:/login";
        try {
            profileService.updateProfile(principal.getName(), fullName, email, phone);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật hồ sơ thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Cập nhật thất bại: " + e.getMessage());
        }
        return "redirect:/admin/profile";
    }

    @PostMapping("/change-password")
    public String changePassword(@org.springframework.web.bind.annotation.RequestParam String oldPassword,
            @org.springframework.web.bind.annotation.RequestParam String newPassword,
            @org.springframework.web.bind.annotation.RequestParam String confirmPassword,
            Principal principal,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        if (principal == null)
            return "redirect:/login";
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("passwordError", "Mật khẩu mới và xác nhận không khớp!");
            return "redirect:/admin/profile";
        }
        try {
            profileService.changePassword(principal.getName(), oldPassword, newPassword);
            redirectAttributes.addFlashAttribute("passwordSuccess", "Đổi mật khẩu thành công!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("passwordError", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("passwordError", "Lỗi trong quá trình đổi mật khẩu.");
        }
        return "redirect:/admin/profile";
    }
}