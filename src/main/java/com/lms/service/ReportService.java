package com.lms.service;

import org.springframework.stereotype.Service;

/**
 * ReportService - Xử lý Logic Báo cáo
 * Người phụ trách: Trần Nguyễn Quốc Anh (CE191655)
 */
@Service
public class ReportService {

    // UC-17.1: Tạo báo cáo tổng hợp
    public void generateReport() {
        // TODO: Implement
    }

    // UC-22.1: Báo cáo doanh thu
    public void generateRevenueReport(String fromDate, String toDate) {
        // TODO: Implement - Tổng hợp từ Transactions
    }

    // UC-22.2: Xuất báo cáo
    public void exportReport(String type, String format) {
        // TODO: Implement - PDF (iText) hoặc Excel (Apache POI)
    }
}
