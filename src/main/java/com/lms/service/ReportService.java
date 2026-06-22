package com.lms.service;

/**
 * ReportService - Xử lý Logic Báo cáo
 * Người phụ trách: Trần Nguyễn Quốc Anh (CE191655)
 */
public interface ReportService {

    // UC-17.1: Tạo báo cáo tổng hợp
    void generateReport();

    // UC-22.1: Báo cáo doanh thu
    void generateRevenueReport(String fromDate, String toDate);

    // UC-22.2: Xuất báo cáo
    void exportReport(String type, String format);

}
