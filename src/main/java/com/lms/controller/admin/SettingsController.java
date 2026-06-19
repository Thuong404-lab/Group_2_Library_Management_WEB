package com.lms.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * SettingsController - Cấu hình Hệ thống
 * Người phụ trách: Trần Ngọc Linh Đang (CE191088)
 */
@Controller
@RequestMapping("/admin/settings")
public class SettingsController {

    // UC-21.1: Manage Borrowing/Return Policies
    @GetMapping
    public String showSettings(Model model) {
        // TODO: Implement - Hiển thị trang cấu hình chính sách
        // TODO: Lấy các SystemSettings hiện tại từ DB
        return "admin/settings";
    }

    @PostMapping("/policies")
    public String updateBorrowingPolicies(@RequestParam Integer maxBorrowDays,
                                           @RequestParam Integer maxRenewals,
                                           @RequestParam Integer maxBooksPerMember,
                                           @RequestParam Double borrowFeePerBook,
                                           Model model) {
        // TODO: Implement - Cập nhật SystemSettings
        // TODO: Ghi log thay đổi vào SystemLogs
        return "redirect:/admin/settings?updated";
    }
}
