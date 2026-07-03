package com.lms.controller.admin;

import com.lms.service.SystemService;
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

    private final SystemService systemService;

    public SettingsController(SystemService systemService) {
        this.systemService = systemService;
    }

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
        // TODO: Ghi log thay đổi vào SystemLogs
        // Lưu settings (ADMIN-only vì endpoint nằm dưới /admin/**)
        systemService.updateBorrowingPolicies(maxBorrowDays, maxRenewals, maxBooksPerMember, borrowFeePerBook);
        return "redirect:/admin/settings?updated";
    }
}
