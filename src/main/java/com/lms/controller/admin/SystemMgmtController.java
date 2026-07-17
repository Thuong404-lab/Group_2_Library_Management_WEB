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
                                 @RequestParam(required = false, defaultValue = "auth") String section,
                                 @RequestParam(required = false, defaultValue = "") String keyword,
                                 @RequestParam(required = false, defaultValue = "") String actionType,
                                 Model model) {

        String currentSection = normalizeSection(section);
        model.addAttribute("logs", systemService.getSystemLogs(page, currentSection, keyword, actionType));
        model.addAttribute("keyword", keyword);
        model.addAttribute("currentSection", currentSection);
        model.addAttribute("actionType", actionType);

        return "admin/system-logs";
    }

    private String normalizeSection(String section) {
        if ("operations".equalsIgnoreCase(section)) {
            return "operations";
        }
        if ("circulation".equalsIgnoreCase(section)) {
            return "circulation";
        }
        return "auth";
    }
}
