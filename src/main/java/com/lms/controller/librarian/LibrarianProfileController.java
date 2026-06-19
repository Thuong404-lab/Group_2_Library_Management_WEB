package com.lms.controller.librarian;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

/**
 * LibrarianProfileController - Quản lý Hồ sơ Thủ thư
 * Người phụ trách: Nguyễn Tiến Thương (CE191329)
 */
@Controller
@RequestMapping("/librarian/profile")
public class LibrarianProfileController {

    // UC-16.1: View Librarian Profile
    @GetMapping
    public String viewProfile(Principal principal, Model model) {
        // TODO: Implement - Lấy thông tin Staff từ Account đang đăng nhập
        return "librarian/profile";
    }

    // UC-16.2: Edit Librarian Profile
    @PostMapping("/update")
    public String updateProfile(Principal principal, Model model) {
        // TODO: Implement - Cập nhật fullName, email, phone của Staff
        return "redirect:/librarian/profile?updated";
    }

    // UC-16.3: Change Librarian Password
    @PostMapping("/change-password")
    public String changePassword(@RequestParam String oldPassword,
                                  @RequestParam String newPassword,
                                  Principal principal, Model model) {
        // TODO: Implement - Tương tự UC-4.3 nhưng cho Librarian
        return "redirect:/librarian/profile?passwordChanged";
    }
}
