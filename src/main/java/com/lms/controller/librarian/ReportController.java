package com.lms.controller.librarian;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * ReportController - Báo cáo & Thống kê
 * Người phụ trách: Trần Nguyễn Quốc Anh (CE191655)
 */
@Controller
@RequestMapping("/librarian/reports")
public class ReportController {

    // UC-17.1: Generate Reports
    @GetMapping
    public String showReportDashboard(Model model) {
        // TODO: Implement - Hiển thị trang báo cáo tổng hợp
        // TODO: Thống kê số lượng sách, số lần mượn, doanh thu
        return "admin/reports";
    }

    // UC-22.1: Generate Revenue Report
    @GetMapping("/revenue")
    public String generateRevenueReport(@RequestParam(required = false) String fromDate,
                                         @RequestParam(required = false) String toDate,
                                         Model model) {
        // TODO: Implement - Tổng hợp doanh thu từ Transactions
        // TODO: Lọc theo khoảng thời gian
        // TODO: Nhóm theo loại giao dịch (BORROW_FEE, FINE, TOP_UP)
        return "librarian/revenue-report";
    }

    // UC-22.2: Export Report
    @GetMapping("/export")
    public String exportReport(@RequestParam String type,
                                @RequestParam String format, Model model) {
        // TODO: Implement - Xuất báo cáo ra PDF hoặc Excel
        // TODO: Sử dụng iText cho PDF, Apache POI cho Excel
        return "redirect:/librarian/reports?exported";
    }
}
