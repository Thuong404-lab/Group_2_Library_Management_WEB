package com.lms.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * SystemMgmtController - Quản lý Hệ thống (Backup/Restore)
 * Người phụ trách: Trần Ngọc Linh Đang (CE191088)
 */
@Controller
@RequestMapping("/admin/system")
public class SystemMgmtController {

    // UC-19.1: Backup Data
    @GetMapping("/backup")
    public String showBackupPage(Model model) {
        // TODO: Implement - Hiển thị trang backup
        // TODO: Hiển thị lịch sử các bản backup trước đó
        return "admin/backup";
    }

    @PostMapping("/backup")
    public String performBackup(Model model) {
        // TODO: Implement - Thực hiện backup database
        // TODO: Xuất file .bak hoặc .sql
        // TODO: Ghi log vào SystemLogs
        return "redirect:/admin/system/backup?success";
    }

    // UC-19.2: Restore Data
    @GetMapping("/restore")
    public String showRestorePage(Model model) {
        // TODO: Implement - Hiển thị trang restore
        return "admin/restore";
    }

    @PostMapping("/restore")
    public String performRestore(Model model) {
        // TODO: Implement - Khôi phục database từ file backup
        // TODO: Cảnh báo: thao tác này sẽ ghi đè dữ liệu hiện tại
        // TODO: Ghi log vào SystemLogs
        return "redirect:/admin/system/restore?success";
    }

    // UC-19.3: View System Logs
    @GetMapping("/logs")
    public String viewSystemLogs(@RequestParam(defaultValue = "0") int page,
                                  @RequestParam(required = false) String action,
                                  Model model) {
        // TODO: Implement - Hiển thị System Logs (phân trang + lọc)
        return "admin/system-logs";
    }
}
