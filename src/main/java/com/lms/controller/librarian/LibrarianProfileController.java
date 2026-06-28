package com.lms.controller.librarian;

import com.lms.entity.User;
import com.lms.service.ProfileService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

/**
 * LibrarianProfileController - Chỉ dành riêng cho vai trò LIBRARIAN
 * Người phụ trách: Nguyễn Tiến Thương (CE191329)
 */
@Controller
@RequestMapping("/librarian/profile")
public class LibrarianProfileController {

    private final ProfileService profileService;

    public LibrarianProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public String viewProfile(Principal principal, Model model) {
        // Spring Security đã lọc quyền trước đó, ở đây chỉ việc lấy đúng thông tin cá nhân
        String username = principal.getName();
        User librarian = profileService.getProfile(username);
        model.addAttribute("librarian", librarian);
        return "librarian/profile";
    }

    @PostMapping("/update")
    public String updateProfile(@RequestParam String fullName,
                                @RequestParam String email,
                                @RequestParam String phone,
                                Principal principal,
                                RedirectAttributes redirectAttributes) {
        try {
            String currentUsername = principal.getName();
            profileService.updateProfile(currentUsername, fullName, email, phone);
            redirectAttributes.addFlashAttribute("successMessage", "Profile updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update: " + e.getMessage());
        }
        return "redirect:/librarian/profile";
    }

    @PostMapping("/change-password")
    public String changePassword(@RequestParam String oldPassword,
                                 @RequestParam String newPassword,
                                 Principal principal,
                                 RedirectAttributes redirectAttributes) {
        try {
            String username = principal.getName();
            profileService.changePassword(username, oldPassword, newPassword);
            redirectAttributes.addFlashAttribute("passwordSuccess", "Password changed successfully!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("passwordError", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("passwordError", "System error, please try again.");
        }
        return "redirect:/librarian/profile";
    }
}