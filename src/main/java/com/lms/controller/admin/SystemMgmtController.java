package com.lms.controller.admin;

import com.lms.service.SystemService;
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
    private final SystemService systemService;

    public SystemMgmtController(SystemService systemService) {
        this.systemService = systemService;
    }

     // UC-19.3: View System Logs
    @GetMapping("/logs")
    public String viewSystemLogs(@RequestParam(defaultValue = "0") int page,
                                 @RequestParam(required = false, defaultValue = "") String action,
                                 @RequestParam(required = false, defaultValue = "") String keyword,
                                 Model model) {

        model.addAttribute("logs", systemService.getSystemLogs(page, action, keyword));
        model.addAttribute("keyword", keyword);

        return "admin/system-logs";
    }
}
