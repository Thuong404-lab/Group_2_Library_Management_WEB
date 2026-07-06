package com.lms.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class LibrarianRevenueReportData {
    private final LocalDate fromDate;
    private final LocalDate toDate;
    private final LocalDateTime generatedAt;
    private final BigDecimal totalRevenue;
    private final long totalTransactions;
    private final BigDecimal averageTransaction;
    private final List<ReportMetric> transactionBreakdown;
    private final List<ReportMetric> monthlyRevenueStats;

    public LibrarianRevenueReportData(LocalDate fromDate,
            LocalDate toDate,
            LocalDateTime generatedAt,
            BigDecimal totalRevenue,
            long totalTransactions,
            BigDecimal averageTransaction,
            List<ReportMetric> transactionBreakdown,
            List<ReportMetric> monthlyRevenueStats) {
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.generatedAt = generatedAt;
        this.totalRevenue = totalRevenue == null ? BigDecimal.ZERO : totalRevenue;
        this.totalTransactions = totalTransactions;
        this.averageTransaction = averageTransaction == null ? BigDecimal.ZERO : averageTransaction;
        this.transactionBreakdown = transactionBreakdown;
        this.monthlyRevenueStats = monthlyRevenueStats;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public LocalDate getToDate() {
        return toDate;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public long getTotalTransactions() {
        return totalTransactions;
    }

    public BigDecimal getAverageTransaction() {
        return averageTransaction;
    }

    public List<ReportMetric> getTransactionBreakdown() {
        return transactionBreakdown;
    }

    public List<ReportMetric> getMonthlyRevenueStats() {
        return monthlyRevenueStats;
    }
}
