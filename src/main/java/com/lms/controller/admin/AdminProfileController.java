package com.lms.controller.admin;
import com.lms.exception.ApplicationException;
import com.lms.controller.LocalizedControllerSupport;

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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.lms.config.CustomUserDetails;

/**
 * AdminProfileController - Chỉ dành riêng cho vai trò ADMIN
 */
@Controller
@RequestMapping("/admin/profile")
public class AdminProfileController extends LocalizedControllerSupport {

    private final ProfileService profileService;

    public AdminProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public String viewAdminProfile(Principal principal, Model model) {
        if (principal == null)
            return "redirect:/login";
        String username = principal.getName();
        if (!model.containsAttribute("admin")) {
            User admin = profileService.getStaffProfile(username);
            model.addAttribute("admin", admin);
        }
        return "admin/profile";
    }

    @PostMapping("/update")
    public String updateProfile(@RequestParam String fullName,
                                @RequestParam String email,
                                @RequestParam String phone,
                                @RequestParam(required = false) org.springframework.web.multipart.MultipartFile avatarFile,
                                Principal principal,
                                RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return "redirect:/login";
        }

        try {
            String currentUsername = principal.getName();
            profileService.updateStaffProfile(currentUsername, fullName, phone, avatarFile);
            
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
                CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();
                User sessionUser = customUserDetails.getUser();
                User updatedUser = profileService.getStaffProfile(currentUsername);
                
                sessionUser.setFullName(updatedUser.getFullName());
                sessionUser.setAvatar(updatedUser.getAvatar());
                sessionUser.setEmail(updatedUser.getEmail());
                sessionUser.setPhone(updatedUser.getPhone());
            }

            redirectAttributes.addFlashAttribute("successMessage", message("backend.profile.updated"));
        } catch (ApplicationException e) {
            if (e instanceof com.lms.exception.ValidationException && ((com.lms.exception.ValidationException) e).getField() != null) {
                String field = ((com.lms.exception.ValidationException) e).getField();
                redirectAttributes.addFlashAttribute(field + "Error", e.getMessage());
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", messageWithDetail("backend.profile.updateFailed", e));
            }
            User tempUser = new User();
            tempUser.setFullName(fullName);
            tempUser.setEmail(email);
            tempUser.setPhone(phone);
            try {
                User current = profileService.getStaffProfile(principal.getName());
                tempUser.setAvatar(current.getAvatar());
                tempUser.setStatus(current.getStatus());
            } catch (Exception ignored) {}
            redirectAttributes.addFlashAttribute("admin", tempUser);
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
            redirectAttributes.addFlashAttribute("passwordError", message("backend.password.mismatch"));
            return "redirect:/admin/profile";
        }
        try {
            profileService.changeStaffPassword(principal.getName(), oldPassword, newPassword);
            redirectAttributes.addFlashAttribute("passwordSuccess", message("backend.password.changed"));
        } catch (ApplicationException e) {
            redirectAttributes.addFlashAttribute("passwordError", e.getMessage());
        }
        return "redirect:/admin/profile";
    }
}
