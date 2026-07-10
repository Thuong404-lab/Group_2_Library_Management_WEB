package com.lms.service;

import com.lms.dto.response.ReportExport;
import com.lms.dto.response.LibrarianRevenueReportData;
import com.lms.dto.response.ReportViewData;

import java.time.LocalDate;

/**
 * ReportService - Xử lý Logic Báo cáo
 * Người phụ trách: Trần Nguyễn Quốc Anh (CE191655)
 */
public interface ReportService {

    ReportViewData getAdminConsoleReport(LocalDate fromDate, LocalDate toDate);

    ReportExport exportAdminReport(LocalDate fromDate, LocalDate toDate, String format);

    LibrarianRevenueReportData getLibrarianRevenueReport(LocalDate fromDate, LocalDate toDate);

    ReportExport exportLibrarianRevenueReport(LocalDate fromDate, LocalDate toDate, String format);

    // UC-17.1: Tạo báo cáo tổng hợp
    void generateReport();

    // UC-22.1: Báo cáo doanh thu
    void generateRevenueReport(String fromDate, String toDate);

    // UC-22.2: Xuất báo cáo
    void exportReport(String type, String format);

}
