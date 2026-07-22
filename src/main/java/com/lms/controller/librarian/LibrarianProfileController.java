package com.lms.controller.librarian;
import com.lms.exception.ApplicationException;
import com.lms.exception.ConflictException;
import com.lms.exception.ValidationException;
import com.lms.controller.LocalizedControllerSupport;

import com.lms.entity.User;
import com.lms.repository.StaffAccountRepository;
import com.lms.service.ProfileService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.lms.config.CustomUserDetails;

/**
 * LibrarianProfileController - Chỉ dành riêng cho vai trò LIBRARIAN
 * Người phụ trách: Nguyễn Tiến Thương (CE191329)
 */
@Controller
@RequestMapping("/librarian/profile")
public class LibrarianProfileController extends LocalizedControllerSupport {

    private final ProfileService profileService;
    private final StaffAccountRepository staffAccountRepository;

    public LibrarianProfileController(ProfileService profileService,
                                      StaffAccountRepository staffAccountRepository) {
        this.profileService = profileService;
        this.staffAccountRepository = staffAccountRepository;
    }

    @GetMapping
    public String viewProfile(Principal principal, Model model) {
        // Spring Security đã lọc quyền trước đó, ở đây chỉ việc lấy đúng thông tin cá nhân
        String username = principal.getName();
        if (!model.containsAttribute("librarian")) {
            User librarian = profileService.getStaffProfile(username);
            model.addAttribute("librarian", librarian);
        }
        model.addAttribute("staffAccount", staffAccountRepository.findByUsername(username).orElse(null));
        return "librarian/profile";
    }

    @PostMapping("/update")
    public String updateProfile(@RequestParam String fullName,
                                @RequestParam String phone,
                                @RequestParam(required = false) org.springframework.web.multipart.MultipartFile avatarFile,
                                Principal principal,
                                RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return "redirect:/staff-login";
        }

        User currentUser = null;
        try {
            String currentUsername = principal.getName();
            currentUser = profileService.getStaffProfile(currentUsername);
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
            String field = fieldOf(e);
            if (field != null) {
                redirectAttributes.addFlashAttribute(field + "Error", e.getMessage());
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", messageWithDetail("backend.profile.updateFailed", e));
            }
            User tempUser = new User();
            tempUser.setFullName(fullName);
            tempUser.setEmail(currentUser == null ? null : currentUser.getEmail());
            tempUser.setPhone(phone);
            if (currentUser != null) {
                tempUser.setAvatar(currentUser.getAvatar());
                tempUser.setStatus(currentUser.getStatus());
            }
            redirectAttributes.addFlashAttribute("librarian", tempUser);
        }
        return "redirect:/librarian/profile";
    }

    @PostMapping("/change-password")
    public String changePassword(@RequestParam String oldPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 Principal principal,
                                 RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return "redirect:/staff-login";
        }
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("passwordError", message("backend.password.mismatch"));
            return "redirect:/librarian/profile";
        }

        try {
            String username = principal.getName();
            profileService.changeStaffPassword(username, oldPassword, newPassword);
            redirectAttributes.addFlashAttribute("passwordSuccess", message("backend.password.changed"));
        } catch (ApplicationException e) {
            redirectAttributes.addFlashAttribute("passwordError", e.getMessage());
        }
        return "redirect:/librarian/profile";
    }

    private String fieldOf(ApplicationException exception) {
        if (exception instanceof ValidationException validationException) {
            return validationException.getField();
        }
        if (exception instanceof ConflictException conflictException) {
            return conflictException.getField();
        }
        return null;
    }
}
