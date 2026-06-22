package com.lms.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * AccountController - Quản lý Tài khoản Hệ thống
 * Người phụ trách: Trần Ngọc Linh Đang (CE191088)
 */
@Controller
@RequestMapping("/admin/accounts")
public class AccountController {

    // UC-20: Hiển thị danh sách tài khoản
    @GetMapping
    public String listAccounts(@RequestParam(defaultValue = "0") int page,
                                Model model) {
        // TODO: Implement - Lấy danh sách Account (phân trang)
        return "admin/accounts";
    }

    // UC-20.4: Search Accounts
    @GetMapping("/search")
    public String searchAccounts(@RequestParam String keyword, Model model) {
        // TODO: Implement - Tìm kiếm Account theo username, email, role
        return "admin/accounts";
    }

    // UC-20.1: Create Account
    @GetMapping("/create")
    public String showCreateForm(Model model) {
        // TODO: Implement - Hiển thị form tạo tài khoản mới
        return "admin/create-account";
    }

    @PostMapping("/create")
    public String createAccount(Model model) {
        // TODO: Implement - Validate input
        // TODO: Mã hóa password bằng BCrypt
        // TODO: Tạo Account + (Member hoặc Staff tùy role)
        // TODO: Nếu là Member → tạo thêm Wallet
        return "redirect:/admin/accounts?created";
    }

    // UC-20.2: Update Account
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Integer id, Model model) {
        // TODO: Implement - Lấy Account theo ID, truyền vào form
        return "admin/edit-account";
    }

    @PostMapping("/edit/{id}")
    public String updateAccount(@PathVariable Integer id, Model model) {
        // TODO: Implement - Cập nhật thông tin Account
        return "redirect:/admin/accounts?updated";
    }

    // UC-20.3: Delete Account
    @PostMapping("/delete/{id}")
    public String deleteAccount(@PathVariable Integer id, Model model) {
        // TODO: Implement - Soft delete (đổi status thành Disabled)
        // TODO: Không cho xóa tài khoản Admin cuối cùng
        return "redirect:/admin/accounts?deleted";
    }

    // UC-20.5: Change Account Status
    @PostMapping("/status/{id}")
    public String changeAccountStatus(@PathVariable Integer id,
                                       @RequestParam String status, Model model) {
        // TODO: Implement - Đổi trạng thái Active/Disabled/Suspended
        return "redirect:/admin/accounts?statusChanged";
    }

    // UC-20.6: Reset Password
    @PostMapping("/reset-password/{id}")
    public String resetPassword(@PathVariable Integer id, Model model) {
        // TODO: Implement - Reset password về mặc định
        // TODO: Mã hóa password mới bằng BCrypt
        return "redirect:/admin/accounts?passwordReset";
    }
}
