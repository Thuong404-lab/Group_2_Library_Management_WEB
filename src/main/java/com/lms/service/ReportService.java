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
}
