package com.lms.controller.admin;

import com.lms.config.CustomUserDetails;
import com.lms.dto.response.AdminStaffListViewData;
import com.lms.service.AdminDashboardService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * DashboardController
 * Người phụ trách: Trần Ngọc Linh Đang (CE191088)
 */

@Controller
@RequestMapping("/admin")
public class DashboardController {

    private final AdminDashboardService dashboardService;

    public DashboardController(AdminDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/dashboard")
    public String viewDashboard(Model model,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        model.addAllAttributes(dashboardService.getDashboardData());
        if (userDetails != null && userDetails.getUser() != null) {
            model.addAttribute("currentUser", userDetails.getUser());
        }
        return "admin/dashboard";
    }

    @GetMapping("/staff")
    public String viewStaffList(@RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "") String status,
            @RequestParam(required = false, defaultValue = "") String staffType,
            Model model) {
        AdminStaffListViewData data = dashboardService.getStaffList(page, keyword, status, staffType);
        model.addAttribute("staffPage", data.staffPage());
        model.addAttribute("keyword", keyword);
        model.addAttribute("accountByUserId", data.accountByUserId());
        model.addAttribute("staffSummary", data.summaryCounts());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedStaffType", staffType);
        return "admin/staff-list";
    }

    @GetMapping("/logs")
    public String viewSystemLogs(@RequestParam(defaultValue = "0") int page,
            Model model) {
        return "admin/system-logs";
    }
}
