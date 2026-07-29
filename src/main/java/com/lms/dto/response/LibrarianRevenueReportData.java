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
    private final BigDecimal totalRefunds;
    private final long totalTransactions;
    private final BigDecimal averageTransaction;
    private final List<ReportMetric> transactionBreakdown;
    private final List<ReportMetric> monthlyRevenueStats;
    private final long activeBorrows;
    private final long pendingReservationRequests;
    private final long depositPaidReservations;
    private final long readyReservations;
    private final long overdueItems;
    private final long totalMembers;

    public LibrarianRevenueReportData(LocalDate fromDate,
            LocalDate toDate,
            LocalDateTime generatedAt,
            BigDecimal totalRevenue,
            BigDecimal totalRefunds,
            long totalTransactions,
            BigDecimal averageTransaction,
            List<ReportMetric> transactionBreakdown,
            List<ReportMetric> monthlyRevenueStats,
            long activeBorrows,
            long pendingReservationRequests,
            long depositPaidReservations,
            long readyReservations,
            long overdueItems,
            long totalMembers) {
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.generatedAt = generatedAt;
        this.totalRevenue = totalRevenue == null ? BigDecimal.ZERO : totalRevenue;
        this.totalRefunds = totalRefunds == null ? BigDecimal.ZERO : totalRefunds;
        this.totalTransactions = totalTransactions;
        this.averageTransaction = averageTransaction == null ? BigDecimal.ZERO : averageTransaction;
        this.transactionBreakdown = transactionBreakdown;
        this.monthlyRevenueStats = monthlyRevenueStats;
        this.activeBorrows = activeBorrows;
        this.pendingReservationRequests = pendingReservationRequests;
        this.depositPaidReservations = depositPaidReservations;
        this.readyReservations = readyReservations;
        this.overdueItems = overdueItems;
        this.totalMembers = totalMembers;
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

    public BigDecimal getTotalRefunds() {
        return totalRefunds;
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

    public long getActiveBorrows() {
        return activeBorrows;
    }

    public long getPendingReservationRequests() {
        return pendingReservationRequests;
    }

    public long getDepositPaidReservations() {
        return depositPaidReservations;
    }

    public long getReadyReservations() {
        return readyReservations;
    }

    public long getOverdueItems() {
        return overdueItems;
    }

    public long getTotalMembers() {
        return totalMembers;
    }
}
