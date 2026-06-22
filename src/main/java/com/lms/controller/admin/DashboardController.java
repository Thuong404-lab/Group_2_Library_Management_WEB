package com.lms.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * DashboardController - Trang Admin tổng quan
 * Người phụ trách: Trần Ngọc Linh Đang (CE191088)
 */
@Controller
@RequestMapping("/admin")
public class DashboardController {

    // UC-18.1: View Admin Dashboard
    @GetMapping("/dashboard")
    public String viewDashboard(Model model) {
        // TODO: Implement - Thống kê tổng quan
        // TODO: Tổng số sách, Tổng thành viên, Sách đang mượn, Doanh thu tháng
        // TODO: Truyền dữ liệu cho biểu đồ Chart.js
        return "admin/dashboard";
    }

    // UC-18.2: View Librarian List
    @GetMapping("/librarians")
    public String viewLibrarianList(Model model) {
        // TODO: Implement - Lấy danh sách Staff (role = Librarian)
        return "admin/librarian-list";
    }

    // UC-18.3: View System Logs (Admin)
    @GetMapping("/logs")
    public String viewSystemLogs(@RequestParam(defaultValue = "0") int page,
                                  Model model) {
        // TODO: Implement - Lấy danh sách SystemLogs (phân trang)
        // TODO: Lọc theo hành động, người thực hiện, thời gian
        return "admin/system-logs";
    }
}
