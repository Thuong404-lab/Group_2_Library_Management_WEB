package com.lms.controller.admin;

import com.lms.dto.response.ReportExport;
import com.lms.dto.response.ReportViewData;
import com.lms.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Controller
@RequestMapping("/admin/reports")
public class AdminReportController {
    private final ReportService reportService;

    public AdminReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    public String showReports(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            Model model) {
        ReportViewData report = reportService.getAdminConsoleReport(fromDate, toDate);
        model.addAttribute("report", report);
        model.addAttribute("fromDate", report.getFromDate());
        model.addAttribute("toDate", report.getToDate());
        return "admin/report-console";
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportReports(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "csv") String format) {
        ReportExport export = reportService.exportAdminReport(fromDate, toDate, format);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(export.getFileName())
                        .build()
                        .toString())
                .header(HttpHeaders.CONTENT_TYPE, export.getContentType())
                .body(export.getContent());
    }
}
