package com.lms.controller.member;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

/**
 * ProfileController - Quản lý Hồ sơ Thành viên
 * Người phụ trách: Nguyễn Tiến Thương (CE191329)
 */
@Controller
@RequestMapping("/member")
public class ProfileController {

    // UC-4.1: View Profile
    @GetMapping("/profile")
    public String viewProfile(Principal principal, Model model) {
        // TODO: Implement - Lấy thông tin Member từ principal.getName()
        // TODO: Truyền Member, Account, Wallet vào model
        return "member/profile";
    }

    // UC-4.2: Update Profile Information
    @PostMapping("/profile/update")
    public String updateProfile(Principal principal, Model model) {
        // TODO: Implement - Validate và cập nhật thông tin Member
        // TODO: Cập nhật fullName, email, phone, address
        return "redirect:/member/profile?updated";
    }

    // UC-4.3: Change Password
    @GetMapping("/change-password")
    public String showChangePasswordForm(Model model) {
        // TODO: Implement - Hiển thị form đổi mật khẩu
        return "member/change-password";
    }

    @PostMapping("/change-password")
    public String changePassword(@RequestParam String oldPassword,
                                  @RequestParam String newPassword,
                                  @RequestParam String confirmPassword,
                                  Principal principal, Model model) {
        // TODO: Implement - Validate mật khẩu cũ
        // TODO: Kiểm tra newPassword == confirmPassword
        // TODO: Mã hóa mật khẩu mới bằng BCrypt, lưu vào Account
        return "redirect:/member/profile?passwordChanged";
    }

    // UC-4.4: View Favorites List
    @GetMapping("/favorites")
    public String viewFavorites(Principal principal, Model model) {
        // TODO: Implement - Lấy danh sách sách yêu thích của Member
        // TODO: Gọi FavoriteRepository.findByMemberId()
        return "member/favorites";
    }
}
