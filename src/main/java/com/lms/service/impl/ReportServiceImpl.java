package com.lms.service.impl;

import com.lms.service.ReportService;

import com.lms.repository.BorrowRepository;
import com.lms.repository.TransactionRepository;
import org.springframework.stereotype.Service;

/**
 * ReportService - Xử lý Logic Báo cáo
 * Người phụ trách: Trần Nguyễn Quốc Anh (CE191655)
 */
@Service
public class ReportServiceImpl implements ReportService {
    private final BorrowRepository borrowRepository;
    private final TransactionRepository transactionRepository;

    public ReportServiceImpl(BorrowRepository borrowRepository, TransactionRepository transactionRepository) {
        this.borrowRepository = borrowRepository;
        this.transactionRepository = transactionRepository;
    }


    // UC-17.1: Tạo báo cáo tổng hợp
    @Override
    public void generateReport() {
        // TODO: Implement
    }

    // UC-22.1: Báo cáo doanh thu
    @Override
    public void generateRevenueReport(String fromDate, String toDate) {
        // TODO: Implement - Tổng hợp từ Transactions
    }

    // UC-22.2: Xuất báo cáo
    @Override
    public void exportReport(String type, String format) {
        // TODO: Implement - PDF (iText) hoặc Excel (Apache POI)
    }
}
