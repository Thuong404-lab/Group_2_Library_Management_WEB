package com.lms.controller.librarian;

import com.lms.dto.response.LibrarianRevenueReportData;
import com.lms.dto.response.ReportExport;
import com.lms.service.ReportService;
import com.lms.service.LibrarianDashboardService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * ReportController - Báo cáo & Thống kê
 * Người phụ trách: Trần Nguyễn Quốc Anh (CE191655)
 */
@Controller
@RequestMapping("/librarian/reports")
public class ReportController {
    private final ReportService reportService;
    private final LibrarianDashboardService dashboardService;

    public ReportController(ReportService reportService, LibrarianDashboardService dashboardService) {
        this.reportService = reportService;
        this.dashboardService = dashboardService;
    }

    // UC-17.1: Librarian revenue report
    @GetMapping
    public String showReportDashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            Model model) {
        // TODO: Implement - Hiển thị trang báo cáo tổng hợp
        // TODO: Thống kê số lượng sách, số lần mượn, doanh thu
        LibrarianRevenueReportData report = reportService.getLibrarianRevenueReport(fromDate, toDate);
        model.addAttribute("report", report);
        model.addAttribute("fromDate", report.getFromDate());
        model.addAttribute("toDate", report.getToDate());
        model.addAttribute("maxDate", LocalDate.now());
        model.addAllAttributes(dashboardService.getStatisticsData());
        return "librarian/revenue-report";
    }

    // Backward-compatible URL for existing revenue links.
    @GetMapping("/revenue")
    public String generateRevenueReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            Model model) {
        // TODO: Implement - Tổng hợp doanh thu từ Transactions
        // TODO: Lọc theo khoảng thời gian
        // TODO: Nhóm theo loại giao dịch (BORROW_FEE, FINE, TOP_UP)
        LibrarianRevenueReportData report = reportService.getLibrarianRevenueReport(fromDate, toDate);
        model.addAttribute("report", report);
        model.addAttribute("fromDate", report.getFromDate());
        model.addAttribute("toDate", report.getToDate());
        model.addAttribute("maxDate", LocalDate.now());
        model.addAllAttributes(dashboardService.getStatisticsData());
        return "librarian/revenue-report";
    }

    // UC-17.2: Export librarian revenue report.
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "csv") String format) {
        // TODO: Implement - Xuất báo cáo ra PDF hoặc Excel
        // TODO: Sử dụng iText cho PDF, Apache POI cho Excel
        ReportExport export = reportService.exportLibrarianRevenueReport(fromDate, toDate, format);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(export.getFileName())
                        .build()
                        .toString())
                .header(HttpHeaders.CONTENT_TYPE, export.getContentType())
                .body(export.getContent());
    }
}
