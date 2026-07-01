package com.lms.controller.member;

import com.lms.entity.User;
import com.lms.service.MemberFavoriteService;
import com.lms.service.ProfileService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

/**
 * ProfileController - Quản lý Hồ sơ Thành viên
 * Người phụ trách: Nguyễn Tiến Thương (CE191329)
 */
@Controller
@RequestMapping("/member")
public class ProfileController {

    private final ProfileService profileService;
    private final MemberFavoriteService memberFavoriteService;

    // Inject ProfileService xử lý logic cốt lõi giống như bên Thủ thư
    public ProfileController(ProfileService profileService,
                             MemberFavoriteService memberFavoriteService) {
        this.profileService = profileService;
        this.memberFavoriteService = memberFavoriteService;
    }

    // UC-4.1: View Profile
    @GetMapping("/profile")
    public String viewProfile(Principal principal, Model model) {
        if (principal == null) {
            return "redirect:/login";
        }

        String username = principal.getName();
        try {
            User member = profileService.getProfile(username);
            model.addAttribute("member", member);
            // Thông tin Account và Wallet có thể lấy trực tiếp thông qua liên kết thực thể member.getAccount() / member.getWallet() ở giao diện Thymeleaf
            return "member/profile";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Không thể tải hồ sơ: " + e.getMessage());
            return "error/500";
        }
    }

    // UC-4.2: Update Profile Information
    @PostMapping("/profile/update")
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
            profileService.updateProfile(currentUsername, fullName, email, phone, avatarFile);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật hồ sơ thành công!");
            return "redirect:/member/profile?updated";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Cập nhật thất bại: " + e.getMessage());
            return "redirect:/member/profile";
        }
    }

    // UC-4.3: Change Password
    @PostMapping("/change-password")
    public String changePassword(@RequestParam String oldPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 Principal principal,
                                 RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return "redirect:/login";
        }

        // Kiểm tra khớp mật khẩu gõ lại ngay tại Controller trước khi gọi xuống Service
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("passwordError", "Mật khẩu mới và xác nhận mật khẩu không trùng khớp!");
            return "redirect:/member/profile";
        }

        try {
            String username = principal.getName();
            profileService.changePassword(username, oldPassword, newPassword);
            redirectAttributes.addFlashAttribute("passwordSuccess", "Thay đổi mật khẩu hệ thống thành công!");
            return "redirect:/member/profile?passwordChanged";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("passwordError", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("passwordError", "Có lỗi xảy ra trong quá trình đổi mật khẩu.");
        }
        return "redirect:/member/profile";
    }

    // UC-4.4: View Favorites List
    @GetMapping("/favorites")
    public String viewFavorites(Principal principal, Model model) {
        if (principal == null) {
            return "redirect:/login";
        }

        model.addAttribute("favorites", memberFavoriteService.getMyFavorites(principal.getName()));

        return "member/favorites";
    }

    @GetMapping("/reviews")
    public String redirectToReviews() {
        return "redirect:/member/interaction/reviews";
    }
}
